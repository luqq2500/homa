package com.homa.reservation.domain;

import com.homa.kernel.Percentage;
import com.homa.reservation.domain.terms.CancellationTier;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The Homa-set values Reservation owns (§11 of the business rules). They are reservation values: a reservation keeps
 * the version in force when it was Requested (P3, RES-8). A change never edits a number in place; it adds a new
 * {@link Version} with a later {@code effectiveFrom}, and every rule reads {@link #asOf(Instant)} at the
 * reservation's {@code requestedAt}.
 *
 * <p>Values another context owns are received, never copied: the 1-hour minimum duration and its 15-minute steps
 * come with Listing's booking bounds, minimum notice and buffer with Listing's terms, the work permit buffer as
 * Provider's {@code rosterableUntil}, and the platform fee and fine deduction cap stay in Payment.
 */
public final class ReservationValues {

    /** The values Homa set first, in force since the beginning. */
    static final Version FIRST = new Version(
            Instant.EPOCH,
            Duration.ofMinutes(15),
            Duration.ofHours(24),
            Duration.ofHours(24),
            Duration.ofHours(1),
            Duration.ofHours(4),
            Duration.ofHours(24),
            Map.of(
                    CancellationTier.FLEXIBLE, Duration.ofHours(4),
                    CancellationTier.STANDARD, Duration.ofHours(12),
                    CancellationTier.FIRM, Duration.ofHours(24)),
            new ProviderFines(Duration.ofHours(24), Percentage.of(10), Percentage.of(12), Percentage.of(20),
                    Percentage.of(30)),
            new CustomerCompensation(Percentage.of(3), Percentage.of(4), Percentage.of(7), Percentage.of(10)),
            Percentage.of(30),
            Percentage.HUNDRED_PERCENT,
            Duration.ofMinutes(15),
            List.of(Duration.ZERO, Duration.ofHours(2)));

    private static final ReservationValues HOMA = new ReservationValues(List.of(FIRST));

    private final List<Version> versions;

    /** A history of versions, oldest first; the first must be in force from {@link Instant#EPOCH}. */
    ReservationValues(List<Version> versions) {
        List<Version> sorted = versions.stream().sorted(Comparator.comparing(Version::effectiveFrom)).toList();
        if (sorted.isEmpty() || !sorted.get(0).effectiveFrom().equals(Instant.EPOCH)) {
            throw new IllegalArgumentException("The first version of the reservation values must start at the epoch");
        }
        this.versions = sorted;
    }

    /** The version in force for a reservation Requested at this moment: the latest one effective by then. */
    public static Version asOf(Instant requestedAt) {
        return HOMA.versionAsOf(requestedAt);
    }

    /** The newest version, which new reservations are Requested under. */
    public static Version current() {
        return HOMA.latest();
    }

    Version versionAsOf(Instant requestedAt) {
        Objects.requireNonNull(requestedAt, "requestedAt");
        return versions.stream()
                .filter(version -> !version.effectiveFrom().isAfter(requestedAt))
                .reduce((earlier, later) -> later)
                .orElseThrow();
    }

    Version latest() {
        return versions.get(versions.size() - 1);
    }

    /**
     * One dated set of Reservation's values (§11).
     *
     * @param effectiveFrom                  reservations Requested from this moment use this version
     * @param checkInPeriod                  the Check-in Period before the job start time: 15 minutes (§1, JOB-1)
     * @param protectionAfterJobEnd          the Protection Window runs until this long after the job end time: 24
     *                                       hours (§1, RPT-1, PAY-2, PAY-6)
     * @param acceptanceResponseTime         the acceptance deadline is this long after the request: 24 hours (ACC-3)
     * @param acceptanceCutOffBeforeCheckIn  ...or this long before the Check-in Period, whichever is first; a Request
     *                                       must also be made at least this long before it: 1 hour (ACC-3)
     * @param gracePeriod                    free cancellation this long after Reserved: 4 hours (CCX-1)
     * @param graceNeedsReservedBeforeCheckIn the grace applies only if Reserved at least this long before the
     *                                       Check-in Period: 24 hours (CCX-1)
     * @param freeCancellationBeforeCheckIn  each tier's free cut-off before the Check-in Period: Flexible 4 hours,
     *                                       Standard 12 hours, Firm 24 hours (CCX-3 to CCX-5)
     * @param providerFines                  the percentages added to the Base Cancellation Fee (PCX, NRS, LMC, NSH)
     * @param compensation                   the percentages added to a full refund (PCX-2, NRS-3, LMC-3, NSH-3)
     * @param customerNoShowShareOfLaterHours a Customer No Show pays this share of the Hours Fee after the first
     *                                       hour: 30% (NSH-5)
     * @param feeAndFineCap                  no fee or fine exceeds this share of the Total Reservation Fee: 100%
     *                                       (CAN-2)
     * @param partialTimeStep                worked time is charged in started steps of this length: 15 minutes
     *                                       (LMC-2, RPT-9, SUS-3)
     * @param completionRemindersAfterJobEnd the provider is reminded this long after the job end time: at the end
     *                                       and 2 hours later (JOB-5)
     */
    public record Version(
            Instant effectiveFrom,
            Duration checkInPeriod,
            Duration protectionAfterJobEnd,
            Duration acceptanceResponseTime,
            Duration acceptanceCutOffBeforeCheckIn,
            Duration gracePeriod,
            Duration graceNeedsReservedBeforeCheckIn,
            Map<CancellationTier, Duration> freeCancellationBeforeCheckIn,
            ProviderFines providerFines,
            CustomerCompensation compensation,
            Percentage customerNoShowShareOfLaterHours,
            Percentage feeAndFineCap,
            Duration partialTimeStep,
            List<Duration> completionRemindersAfterJobEnd) {

        public Version {
            Objects.requireNonNull(effectiveFrom, "effectiveFrom");
            Objects.requireNonNull(checkInPeriod, "checkInPeriod");
            Objects.requireNonNull(protectionAfterJobEnd, "protectionAfterJobEnd");
            Objects.requireNonNull(acceptanceResponseTime, "acceptanceResponseTime");
            Objects.requireNonNull(acceptanceCutOffBeforeCheckIn, "acceptanceCutOffBeforeCheckIn");
            Objects.requireNonNull(gracePeriod, "gracePeriod");
            Objects.requireNonNull(graceNeedsReservedBeforeCheckIn, "graceNeedsReservedBeforeCheckIn");
            freeCancellationBeforeCheckIn = Map.copyOf(freeCancellationBeforeCheckIn);
            Objects.requireNonNull(providerFines, "providerFines");
            Objects.requireNonNull(compensation, "compensation");
            Objects.requireNonNull(customerNoShowShareOfLaterHours, "customerNoShowShareOfLaterHours");
            Objects.requireNonNull(feeAndFineCap, "feeAndFineCap");
            Objects.requireNonNull(partialTimeStep, "partialTimeStep");
            completionRemindersAfterJobEnd = List.copyOf(completionRemindersAfterJobEnd);
        }
    }

    /**
     * Provider fines, as percentages of the Total Reservation Fee added to the Base Cancellation Fee (§6, §7). A
     * provider cancelling more than {@code lateCancellationWithin} before the Check-in Period is fined the Base alone
     * (PCX-1).
     *
     * @param lateCancellationWithin the Check-in Period starts within this time: 24 hours (PCX-2)
     * @param lateCancellation       Base + 10% (PCX-2)
     * @param noRoster               Base + 12% (NRS-2)
     * @param lastMinute             Base + 20% (LMC-3)
     * @param noShow                 Base + 30% (NSH-3)
     */
    public record ProviderFines(
            Duration lateCancellationWithin,
            Percentage lateCancellation,
            Percentage noRoster,
            Percentage lastMinute,
            Percentage noShow) {

        public ProviderFines {
            Objects.requireNonNull(lateCancellationWithin, "lateCancellationWithin");
            Objects.requireNonNull(lateCancellation, "lateCancellation");
            Objects.requireNonNull(noRoster, "noRoster");
            Objects.requireNonNull(lastMinute, "lastMinute");
            Objects.requireNonNull(noShow, "noShow");
        }
    }

    /**
     * Customer compensation, as percentages of the Total Reservation Fee paid on top of a full refund (PAY-4).
     *
     * @param lateProviderCancellation 3% (PCX-2)
     * @param noRoster                 4% (NRS-3)
     * @param providerLastMinute       7% (LMC-3)
     * @param providerNoShow           10% (NSH-3)
     */
    public record CustomerCompensation(
            Percentage lateProviderCancellation,
            Percentage noRoster,
            Percentage providerLastMinute,
            Percentage providerNoShow) {

        public CustomerCompensation {
            Objects.requireNonNull(lateProviderCancellation, "lateProviderCancellation");
            Objects.requireNonNull(noRoster, "noRoster");
            Objects.requireNonNull(providerLastMinute, "providerLastMinute");
            Objects.requireNonNull(providerNoShow, "providerNoShow");
        }
    }
}
