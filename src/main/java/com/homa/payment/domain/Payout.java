package com.homa.payment.domain;

import com.homa.kernel.Money;
import com.homa.payment.domain.exception.PayoutNotAwaitingCompletionException;
import com.homa.payment.domain.exception.PayoutNotPendingException;
import com.homa.payment.domain.exception.PayoutNotReleasableException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What a provider is owed for one reservation, waiting to be released. It is opened as a JOB payout when the
 * reservation is Reserved, so a report made before the job is marked Completed already holds it (RPT-6). Its gross
 * starts at the reservation total and drops by every partial refund (ROS-8, RPT-9, SUS-3, a refund Support decides),
 * never below zero.
 *
 * <p>A JOB payout is releasable once the job is marked Completed, the Protection Window has ended and no report is
 * open (PAY-2, JOB-5). If the reservation ends with a cancellation, last-minute or no-show fee for the provider, the
 * payout pays that fee instead; a FEE payout needs no completion and waits for the booked job's Protection Window
 * and every report the same way (PAY-6). Releasing takes Homa's platform fee from every payout (PAY-3), at the rate in
 * force when the reservation was Requested (P3), rounded once to the sen (PAY-8). Amounts come from the Settlement;
 * Payment never re-derives them.
 */
public final class Payout {

    private final PayoutId id;
    private final ReservationId reservationId;
    private final ProviderId providerId;
    private final Instant requestedAt;
    private final Instant protectionWindowEnd;
    private final Set<ReportId> openReports = new HashSet<>();
    private PayoutKind kind = PayoutKind.JOB;
    private Money gross;
    private Instant completedAt;
    private PayoutStatus status = PayoutStatus.PENDING;
    private PayoutRelease release;

    private Payout(PayoutId id, ReservationId reservationId, ProviderId providerId, Money gross, Instant requestedAt,
            Instant protectionWindowEnd) {
        this.id = Objects.requireNonNull(id, "id");
        this.reservationId = Objects.requireNonNull(reservationId, "reservationId");
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.gross = Objects.requireNonNull(gross, "gross");
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
        this.protectionWindowEnd = Objects.requireNonNull(protectionWindowEnd, "protectionWindowEnd");
    }

    /**
     * The reservation is Reserved: the provider's JOB payout opens for the reservation total, waiting for the job to
     * be marked Completed and the Protection Window to end (PAY-2). Reports from now on hold it (RPT-6).
     */
    public static Payout openForReservation(PayoutId id, ReservationId reservationId, ProviderId providerId,
            Money total, Instant requestedAt, Instant protectionWindowEnd) {
        return new Payout(id, reservationId, providerId, Amounts.requirePayable(total, "A payout"), requestedAt,
                protectionWindowEnd);
    }

    /**
     * Rebuilds a payout from what was stored: its kind and gross, which partial refunds may have brought down to
     * zero, when the job was marked Completed, the reports still open, and its status with the release lines once
     * released ({@code null} for a moment or release not reached). Only the shape is checked: a gross that isn't
     * negative, release lines exactly once released, and no completion on a payout that pays a fee instead (PAY-6).
     * Whether it was releasable isn't checked again: the Protection Window and reports held when it was released.
     */
    public static Payout restore(PayoutId id, ReservationId reservationId, ProviderId providerId, PayoutKind kind,
            Money gross, Instant requestedAt, Instant protectionWindowEnd, Instant completedAt,
            Set<ReportId> openReports, PayoutStatus status, PayoutRelease release) {
        Payout payout = new Payout(id, reservationId, providerId, gross, requestedAt, protectionWindowEnd);
        payout.kind = Objects.requireNonNull(kind, "kind");
        payout.completedAt = completedAt;
        payout.openReports.addAll(openReports);
        payout.status = Objects.requireNonNull(status, "status");
        payout.release = release;
        payout.requireStoredShape();
        payout.requireNoCompletionOfAFee();
        return payout;
    }

    private void requireStoredShape() {
        requireShape(!gross.isNegative(), "a gross that isn't negative");
        requireShape(released().isPresent() == (status == PayoutStatus.RELEASED),
                "release lines exactly once it is released");
    }

    private void requireNoCompletionOfAFee() {
        requireShape(kind == PayoutKind.JOB || completedAt().isEmpty(), "no completion when it pays a fee instead");
    }

    private static void requireShape(boolean fits, String what) {
        if (!fits) {
            throw new IllegalArgumentException("A stored payout needs " + what);
        }
    }

    /** The provider marked the job Completed (JOB-3). Even marked late, it is releasable once due (JOB-5). */
    public void markCompleted(Instant completedAt) {
        Objects.requireNonNull(completedAt, "completedAt");
        requireAwaitingCompletion();
        this.completedAt = completedAt;
    }

    /**
     * The reservation ended before the job was done, and the customer pays the provider a cancellation, last-minute
     * or no-show fee: the payout pays that fee instead. It keeps the booked job's Protection Window and every open
     * report (PAY-6, RPT-6).
     */
    public void payFeeInstead(Money fee) {
        Amounts.requirePayable(fee, "A fee payout");
        requireAwaitingCompletion();
        kind = PayoutKind.FEE;
        gross = fee;
    }

    /**
     * The reservation was cancelled, and Reservation worked out what the customer pays the provider from it: a
     * cancellation, last-minute or no-show fee, or nothing. A fee turns the payout into a FEE payout (PAY-6); nothing
     * voids it. Only the payout's own state decides which jobs this reaches: a job still waiting to be marked Completed
     * can turn into a fee payout or be voided, and a job already marked Completed can only be voided, because a Provider
     * No Show confirmed after Completed means the job was never done (NSH-6). A payout already released, voided or
     * turned into a fee payout is refused: a reservation is settled once.
     */
    public void settleCancellation(Money providerPayoutGross) {
        Objects.requireNonNull(providerPayoutGross, "providerPayoutGross");
        if (providerPayoutGross.isZero()) {
            makeVoid();
        } else {
            payFeeInstead(providerPayoutGross);
        }
    }

    /**
     * Part of the customer's payment was refunded (ROS-8, RPT-9, SUS-3, or a refund Support decided): the provider is
     * owed that much less, never below zero.
     */
    public void reduceByRefund(Money refund) {
        Amounts.requirePayable(refund, "A refund");
        requirePending();
        gross = gross.minus(refund).max(Money.zero(gross.currency()));
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

    /**
     * True while the payout is waiting and nothing holds it any more: the job is marked Completed (or it pays a fee),
     * the Protection Window has ended and no report is open (PAY-2, PAY-6, RPT-6).
     */
    public boolean releasableAt(Instant now) {
        Objects.requireNonNull(now, "now");
        return status == PayoutStatus.PENDING && isOwed() && !now.isBefore(protectionWindowEnd)
                && openReports.isEmpty();
    }

    private boolean isOwed() {
        return kind == PayoutKind.FEE || completedAt != null;
    }

    /**
     * Releases the payout to the provider: the platform fee comes off every payout (PAY-3), at the rate in force when
     * the reservation was Requested (P3), rounded once to the sen (PAY-8).
     */
    public PayoutRelease release(Instant now) {
        return release(now, PaymentValues.homa());
    }

    PayoutRelease release(Instant now, PaymentValues values) {
        if (!releasableAt(now)) {
            throw new PayoutNotReleasableException(id, now);
        }
        Money platformFee = PlatformFeeRate.of(values.inForceAt(requestedAt)).feeOn(gross);
        release = new PayoutRelease(id, providerId, kind, gross, platformFee, now);
        status = PayoutStatus.RELEASED;
        return release;
    }

    /**
     * The payout will never be released: the reservation ended with nothing owed to the provider, or a Provider No
     * Show was confirmed after the job was marked Completed (NSH-6). Only a payout still waiting can be voided.
     */
    public void makeVoid() {
        requirePending();
        status = PayoutStatus.VOID;
    }

    private void requireAwaitingCompletion() {
        requirePending();
        if (kind != PayoutKind.JOB || completedAt != null) {
            throw new PayoutNotAwaitingCompletionException(id);
        }
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

    /** What the provider is owed before the platform fee: the reservation total less every partial refund, or the fee. */
    public Money gross() {
        return gross;
    }

    /** When the reservation was Requested: the Homa-set values in force then apply to this payout (P3). */
    public Instant requestedAt() {
        return requestedAt;
    }

    public Instant protectionWindowEnd() {
        return protectionWindowEnd;
    }

    /** The reports on the reservation still open: while any is, the payout waits (RPT-6). */
    public Set<ReportId> openReports() {
        return Set.copyOf(openReports);
    }

    /** When the job was marked Completed, once it has been. */
    public Optional<Instant> completedAt() {
        return Optional.ofNullable(completedAt);
    }

    public PayoutStatus status() {
        return status;
    }

    /** The release's money lines, once the payout has been released. */
    public Optional<PayoutRelease> released() {
        return Optional.ofNullable(release);
    }
}
