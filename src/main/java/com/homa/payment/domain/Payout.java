package com.homa.payment.domain;

import com.homa.kernel.Money;
import com.homa.payment.domain.exception.PayoutNotPendingException;
import com.homa.payment.domain.exception.PayoutNotReleasableException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What a provider is owed for one reservation, waiting to be released. A JOB payout is opened when the job is marked
 * Completed, so the completion condition of PAY-2 holds from its creation (and JOB-5: a job marked Completed late
 * opens a payout that is releasable at once). A FEE payout (PAY-6) needs no completion.
 *
 * <p>It is releasable once the Protection Window has ended and no report on the reservation is open (PAY-2, RPT-6).
 * Releasing takes Homa's 15.5% platform fee from a JOB payout and nothing from a FEE payout (PAY-3), rounded once to
 * the sen (PAY-8). The gross comes from the Settlement; Payment never re-derives it.
 */
public final class Payout {

    private final PayoutId id;
    private final ReservationId reservationId;
    private final ProviderId providerId;
    private final PayoutKind kind;
    private final Money gross;
    private final Instant protectionWindowEnd;
    private final Set<ReportId> openReports = new HashSet<>();
    private PayoutStatus status = PayoutStatus.PENDING;
    private PayoutRelease release;

    private Payout(PayoutId id, ReservationId reservationId, ProviderId providerId, PayoutKind kind, Money gross,
            Instant protectionWindowEnd) {
        this.id = Objects.requireNonNull(id, "id");
        this.reservationId = Objects.requireNonNull(reservationId, "reservationId");
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.kind = kind;
        this.gross = Amounts.requirePayable(gross, "A payout");
        this.protectionWindowEnd = Objects.requireNonNull(protectionWindowEnd, "protectionWindowEnd");
    }

    /** The job was marked Completed: the provider's earnings wait for the end of the Protection Window (PAY-2). */
    public static Payout forCompletedJob(PayoutId id, ReservationId reservationId, ProviderId providerId,
            Money gross, Instant protectionWindowEnd) {
        return new Payout(id, reservationId, providerId, PayoutKind.JOB, gross, protectionWindowEnd);
    }

    /**
     * The customer paid the provider a cancellation, last-minute or no-show fee: it waits for the end of the booked
     * job's Protection Window, the same as a job payout (PAY-6).
     */
    public static Payout forFee(PayoutId id, ReservationId reservationId, ProviderId providerId, Money fee,
            Instant protectionWindowEnd) {
        return new Payout(id, reservationId, providerId, PayoutKind.FEE, fee, protectionWindowEnd);
    }

    /** A report on the reservation was opened: the payout waits until it is resolved (RPT-6). */
    public void reportOpened(ReportId reportId) {
        Objects.requireNonNull(reportId, "reportId");
        requirePending();
        openReports.add(reportId);
    }

    /** A report on the reservation was resolved. A report this payout never saw open changes nothing. */
    public void reportResolved(ReportId reportId) {
        openReports.remove(Objects.requireNonNull(reportId, "reportId"));
    }

    /** True when the Protection Window has ended, no report is open, and the payout is still waiting (PAY-2). */
    public boolean releasableAt(Instant now) {
        return status == PayoutStatus.PENDING && !now.isBefore(protectionWindowEnd) && openReports.isEmpty();
    }

    /**
     * Releases the payout to the provider: the platform fee comes off a JOB payout only (PAY-3), rounded once to the
     * sen (PAY-8).
     */
    public PayoutRelease release(Instant now) {
        if (!releasableAt(now)) {
            throw new PayoutNotReleasableException(id, now);
        }
        Money platformFee = kind == PayoutKind.JOB ? PlatformFeeRate.HOMA.feeOn(gross) : Money.zero(gross.currency());
        release = new PayoutRelease(id, providerId, kind, gross, platformFee, now);
        status = PayoutStatus.RELEASED;
        return release;
    }

    /**
     * The payout will never be released, for a Provider No Show confirmed after the job was marked Completed (NSH-6,
     * slice 5). Only a payout still waiting can be voided.
     */
    public void makeVoid() {
        requirePending();
        status = PayoutStatus.VOID;
    }

    private void requirePending() {
        if (status != PayoutStatus.PENDING) {
            throw new PayoutNotPendingException(id, status);
        }
    }

    public PayoutId id() {
        return id;
    }

    public ReservationId reservationId() {
        return reservationId;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public PayoutKind kind() {
        return kind;
    }

    /** What the Settlement gave the provider, before the platform fee. */
    public Money gross() {
        return gross;
    }

    public Instant protectionWindowEnd() {
        return protectionWindowEnd;
    }

    public PayoutStatus status() {
        return status;
    }

    /** The release's money lines, once the payout has been released. */
    public Optional<PayoutRelease> released() {
        return Optional.ofNullable(release);
    }
}
