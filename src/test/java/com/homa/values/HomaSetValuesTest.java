package com.homa.values;

import static org.assertj.core.api.Assertions.assertThat;

import com.homa.customer.domain.CustomerValues;
import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import com.homa.listing.domain.ListingValues;
import com.homa.payment.domain.PaymentValues;
import com.homa.provider.domain.ProviderValues;
import com.homa.reservation.domain.ReservationValues;
import com.homa.reservation.domain.terms.CancellationTier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Checks every row of §13 "Homa-set values" in docs/homa-business-rule.md against the one constant that owns it
 * (plan, "Homa-set values"), so the rules page and the code can't drift apart silently. The rows are read from the
 * rules page itself: a row added there fails {@link #everyHomaSetValueHasAnOwnerOrIsPending()} until it is checked
 * here or listed as pending.
 *
 * <p>Reservation values are dated versions read with {@code asOf(requestedAt)} (P3). That a reservation Requested
 * before a change keeps the old version is tested beside each owner, in {@code ReservationValuesTest} and
 * {@code PaymentValuesTest}, which can build a later version; here every check reads the version in force today.
 */
class HomaSetValuesTest {

    private static final Path RULES = Path.of("docs/homa-business-rule.md");

    /** §13 rows whose owning context is not built yet; each must move into {@link #checks()} with its slice. */
    private static final Set<String> PENDING = Set.of(
            "Rating Window",      // Rating, slice 7 (RTG-2)
            "Ratings",            // Rating, slice 7 (RTG-4, RTG-7, RTG-8)
            "Reliability counts", // Rating, slice 7 (RTG-10)
            // Retention periods (§10) are account values owned by the context that stores each record; their
            // retention types arrive with slice 9 (plan, "Data retention").
            "Retention: payment records",                                        // Payment, DAT-2, DAT-6
            "Retention: damage and safety reports",                              // Support, DAT-3
            "Retention: other reports, reliability records, chats and photos",   // Support, Rating, Conversation,
                                                                                  // Reservation, DAT-3 to DAT-5
            "Retention: reservation address and arrival note",                   // Reservation, DAT-8
            "Retention: verification results",                                   // Provider, DAT-9
            "Retention: closed provider's ratings"                               // Rating, DAT-10
    );

    /**
     * §13 rows that name a formula over the reservation's own terms rather than a Homa-set number: the Customer
     * cancellation fee is the Base Cancellation Fee. Their amounts are checked by the settlement policy tests (CCX-6,
     * LMC-1) in slice 3.
     */
    private static final Set<String> FORMULAS = Set.of("Customer cancellation fee");

    private static final ReservationValues.Version RESERVATION = ReservationValues.current();
    private static final PaymentValues.Version PAYMENT = PaymentValues.current();

    private static Map<String, Runnable> checks() {
        Map<String, Runnable> checks = new LinkedHashMap<>();
        checks.put("Minimum helper age", () -> assertThat(ProviderValues.MINIMUM_HELPER_AGE_YEARS).isEqualTo(18));
        checks.put("Work permit buffer", () -> assertThat(ProviderValues.WORK_PERMIT_BUFFER_DAYS).isEqualTo(90));
        checks.put("Re-verification cycle", () -> {
            assertThat(ProviderValues.REVERIFICATION_CYCLE_MONTHS).isEqualTo(6);
            assertThat(ProviderValues.REVERIFICATION_REMINDER_DAYS).isEqualTo(60);
        });
        checks.put("Re-verification when flagged",
                () -> assertThat(ProviderValues.FLAGGED_REVERIFICATION_DAYS).isEqualTo(14));
        checks.put("Customer suspension lengths", () -> {
            assertThat(CustomerValues.SHORT_SUSPENSION_DAYS).isEqualTo(7);
            assertThat(CustomerValues.LONG_SUSPENSION_DAYS).isEqualTo(30);
        });
        checks.put("Minimum rate", () -> assertThat(ListingValues.MINIMUM_RATE).isEqualTo(Money.myr(20)));
        checks.put("Maximum discount", () -> assertThat(ListingValues.MAXIMUM_DISCOUNT).isEqualTo(Percentage.of(99)));
        checks.put("Default hours", () -> {
            assertThat(ListingValues.DEFAULT_MIN_HOURS).isEqualTo(Duration.ofHours(1));
            assertThat(ListingValues.DEFAULT_MAX_HOURS).isEqualTo(Duration.ofHours(8));
        });
        checks.put("Default helpers", () -> assertThat(ListingValues.DEFAULT_MIN_HELPERS).isEqualTo(1));
        checks.put("Minimum notice", () -> {
            assertThat(ListingValues.DEFAULT_MINIMUM_NOTICE).isEqualTo(Duration.ofHours(2));
            assertThat(ListingValues.INSTANT_NOTICE_FLOOR).isEqualTo(Duration.ofHours(1));
            assertThat(ListingValues.REQUEST_NOTICE_FLOOR).isEqualTo(Duration.ofHours(2));
        });
        checks.put("Buffer between jobs",
                () -> assertThat(ListingValues.DEFAULT_JOB_BUFFER).isEqualTo(Duration.ofMinutes(30)));
        checks.put("Duration", () -> {
            assertThat(ListingValues.MINIMUM_DURATION).isEqualTo(Duration.ofHours(1));
            assertThat(ListingValues.DURATION_STEP).isEqualTo(Duration.ofMinutes(15));
        });
        checks.put("Partial-time charging",
                () -> assertThat(RESERVATION.partialTimeStep()).isEqualTo(Duration.ofMinutes(15)));
        checks.put("Check-in Period", () -> assertThat(RESERVATION.checkInPeriod()).isEqualTo(Duration.ofMinutes(15)));
        checks.put("Acceptance deadline", () -> {
            assertThat(RESERVATION.acceptanceResponseTime()).isEqualTo(Duration.ofHours(24));
            assertThat(RESERVATION.acceptanceCutOffBeforeCheckIn()).isEqualTo(Duration.ofHours(1));
        });
        checks.put("Grace Period", () -> {
            assertThat(RESERVATION.gracePeriod()).isEqualTo(Duration.ofHours(4));
            assertThat(RESERVATION.graceNeedsReservedBeforeCheckIn()).isEqualTo(Duration.ofHours(24));
        });
        checks.put("Cancellation tiers", () -> assertThat(RESERVATION.freeCancellationBeforeCheckIn()).isEqualTo(Map.of(
                CancellationTier.FLEXIBLE, Duration.ofHours(4),
                CancellationTier.STANDARD, Duration.ofHours(12),
                CancellationTier.FIRM, Duration.ofHours(24))));
        checks.put("Provider fines", () -> {
            ReservationValues.ProviderFines fines = RESERVATION.providerFines();
            assertThat(fines.lateCancellationWithin()).isEqualTo(Duration.ofHours(24));
            assertThat(fines.lateCancellation()).isEqualTo(Percentage.of(10));
            assertThat(fines.noRoster()).isEqualTo(Percentage.of(12));
            assertThat(fines.lastMinute()).isEqualTo(Percentage.of(20));
            assertThat(fines.noShow()).isEqualTo(Percentage.of(30));
        });
        checks.put("Customer compensation", () -> {
            ReservationValues.CustomerCompensation compensation = RESERVATION.compensation();
            assertThat(List.of(compensation.lateProviderCancellation(), compensation.noRoster(),
                    compensation.providerLastMinute(), compensation.providerNoShow()))
                    .containsExactly(Percentage.of(3), Percentage.of(4), Percentage.of(7), Percentage.of(10));
        });
        checks.put("Customer no-show fee",
                () -> assertThat(RESERVATION.customerNoShowShareOfLaterHours()).isEqualTo(Percentage.of(30)));
        checks.put("Fee and fine cap", () -> assertThat(RESERVATION.feeAndFineCap()).isEqualTo(Percentage.of(100)));
        checks.put("Protection Window",
                () -> assertThat(RESERVATION.protectionAfterJobEnd()).isEqualTo(Duration.ofHours(24)));
        checks.put("Completion reminders", () -> assertThat(RESERVATION.completionRemindersAfterJobEnd())
                .containsExactly(Duration.ZERO, Duration.ofHours(2)));
        checks.put("Platform fee", () -> assertThat(PAYMENT.platformFeeRate()).isEqualTo(Percentage.of("15.5")));
        checks.put("Fine deduction cap", () -> assertThat(PAYMENT.fineDeductionCap()).isEqualTo(Percentage.of(20)));
        checks.put("Money rounding",
                () -> assertThat(Money.myr("39.375").roundedToSen()).isEqualTo(Money.myr("39.38")));
        return checks;
    }

    private static List<String> rowsOfSection11() throws IOException {
        List<String> lines = Files.readAllLines(RULES);
        int start = lines.indexOf("## 13. Homa-set values");
        assertThat(start).as("§13 heading in %s", RULES).isNotNegative();
        return lines.subList(start + 1, lines.size()).stream()
                .takeWhile(line -> !line.startsWith("## "))
                .filter(line -> line.startsWith("| ") && !line.startsWith("| Value ") && !line.startsWith("| ---"))
                .map(line -> line.split("\\|")[1].strip())
                .toList();
    }

    @Test
    @DisplayName("Every value in §13 of the rules page is checked against its owner here, or listed as a formula or pending.")
    void everyHomaSetValueHasAnOwnerOrIsPending() throws IOException {
        List<String> rows = rowsOfSection11();

        assertThat(rows).isNotEmpty();
        assertThat(rows).allSatisfy(row -> assertThat(checks().containsKey(row) || PENDING.contains(row) || FORMULAS.contains(row))
                .as("§13 row '%s' has no owner check and is not pending", row).isTrue());
        assertThat(checks().keySet()).as("checks for rows no longer in §13").isSubsetOf(rows);
        assertThat(PENDING).as("pending rows no longer in §13").isSubsetOf(rows);
        assertThat(FORMULAS).as("formula rows no longer in §13").isSubsetOf(rows);
    }

    @TestFactory
    @DisplayName("Each owner's constant matches §13.")
    Stream<DynamicTest> eachOwnersConstantMatchesTheRulesPage() {
        return checks().entrySet().stream()
                .map(check -> DynamicTest.dynamicTest(check.getKey(), check.getValue()::run));
    }

    @Test
    @DisplayName("Reservation values are read as of the reservation's request time; today only the launch version exists.")
    void reservationValuesAreReadAsOfTheRequestTime() {
        Instant requestedAt = Instant.parse("2026-11-30T12:00:00Z");

        assertThat(ReservationValues.asOf(requestedAt)).isEqualTo(RESERVATION);
        assertThat(PaymentValues.asOf(requestedAt)).isEqualTo(PAYMENT);
        assertThat(RESERVATION.effectiveFrom()).isEqualTo(Instant.EPOCH);
        assertThat(PAYMENT.effectiveFrom()).isEqualTo(Instant.EPOCH);
    }
}
