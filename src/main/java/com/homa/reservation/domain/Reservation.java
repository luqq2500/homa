package com.homa.reservation.domain;

import com.homa.kernel.Money;
import com.homa.reservation.domain.ReservationValues.Version;
import com.homa.reservation.domain.exception.AcceptanceDeadlineNotReachedException;
import com.homa.reservation.domain.exception.AcceptanceDeadlinePassedException;
import com.homa.reservation.domain.exception.BookingRefusedException;
import com.homa.reservation.domain.exception.CompletionPhotoRequiredException;
import com.homa.reservation.domain.exception.JobAlreadyEndedException;
import com.homa.reservation.domain.exception.JobNotEndedException;
import com.homa.reservation.domain.exception.MemberNotRosterableException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.domain.exception.RosterLockedException;
import com.homa.reservation.domain.pricing.Quote;
import com.homa.reservation.domain.pricing.ReservationPricing;
import com.homa.reservation.domain.replica.BookableProvider;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.domain.replica.RosterableMember;
import com.homa.reservation.domain.roster.Roster;
import com.homa.reservation.domain.schedule.CustomerSchedule;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import com.homa.reservation.domain.settlement.CustomerLastMinutePolicy;
import com.homa.reservation.domain.settlement.FreeCancellationPolicy;
import com.homa.reservation.domain.settlement.LateCustomerCancellationPolicy;
import com.homa.reservation.domain.settlement.ProviderCancellationPolicy;
import com.homa.reservation.domain.settlement.ProviderLastMinutePolicy;
import com.homa.reservation.domain.settlement.Settlement;
import com.homa.reservation.domain.settlement.SettlementBasis;
import com.homa.reservation.domain.settlement.SettlementPolicy;
import com.homa.reservation.domain.terms.AcceptanceDeadline;
import com.homa.reservation.domain.terms.GracePeriod;
import com.homa.reservation.domain.terms.ReservationTerms;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A reservation: the core aggregate. It is made only when a payment is confirmed (RES-7, P1), keeps the provider's
 * terms, Homa's reservation values and the customer's address from when it was Requested (RES-8, P3), and moves
 * Requested → Reserved → Completed, or ends Withdrawn, Declined, Expired or Cancelled. It is never edited: a change is
 * a new booking (P2, REQ-1).
 */
public final class Reservation {

    /** The statuses a stored reservation can have so far, each with the statuses it has been through to get there. */
    private static final Map<ReservationStatus, Set<ReservationStatus>> PASSED_THROUGH = Map.of(
            ReservationStatus.REQUESTED, Set.of(),
            ReservationStatus.RESERVED, Set.of(ReservationStatus.RESERVED),
            ReservationStatus.COMPLETED, Set.of(ReservationStatus.RESERVED, ReservationStatus.COMPLETED),
            ReservationStatus.WITHDRAWN, Set.of(ReservationStatus.WITHDRAWN),
            ReservationStatus.DECLINED, Set.of(ReservationStatus.DECLINED),
            ReservationStatus.EXPIRED, Set.of(ReservationStatus.EXPIRED),
            ReservationStatus.CANCELLED, Set.of(ReservationStatus.RESERVED, ReservationStatus.CANCELLED));

    private static final SettlementPolicy FREE = new FreeCancellationPolicy();
    private static final SettlementPolicy LATE_CUSTOMER = new LateCustomerCancellationPolicy();
    private static final SettlementPolicy CUSTOMER_LAST_MINUTE = new CustomerLastMinutePolicy();
    private static final SettlementPolicy PROVIDER_CANCELLATION = new ProviderCancellationPolicy();
    private static final SettlementPolicy PROVIDER_LAST_MINUTE = new ProviderLastMinutePolicy();

    private final ReservationId id;
    private final CheckoutId checkoutId;
    private final Booking booking;
    private final ReservationTerms terms;
    private final Quote quote;
    private final Version values;
    private final Instant requestedAt;
    private final AcceptanceDeadline acceptanceDeadline;
    private ReservationStatus status;
    private Instant reservedAt;
    private Instant withdrawnAt;
    private Instant declinedAt;
    private Instant expiredAt;
    private Cancellation cancellation;
    private Roster roster;
    private Instant completedAt;
    private List<CompletionPhoto> completionPhotos;
    private final long version;

    private Reservation(ReservationId id, CheckoutId checkoutId, Booking booking, ReservationTerms terms, Quote quote,
            Version values, Instant requestedAt, AcceptanceDeadline acceptanceDeadline, ReservationStatus status,
            Instant reservedAt, Instant withdrawnAt, Instant declinedAt, Instant expiredAt, Cancellation cancellation,
            Roster roster, Instant completedAt, List<CompletionPhoto> completionPhotos, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.checkoutId = Objects.requireNonNull(checkoutId, "checkoutId");
        this.booking = Objects.requireNonNull(booking, "booking");
        this.terms = Objects.requireNonNull(terms, "terms");
        this.quote = Objects.requireNonNull(quote, "quote");
        this.values = Objects.requireNonNull(values, "values");
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
        this.acceptanceDeadline = acceptanceDeadline;
        this.status = Objects.requireNonNull(status, "status");
        this.reservedAt = reservedAt;
        this.withdrawnAt = withdrawnAt;
        this.declinedAt = declinedAt;
        this.expiredAt = expiredAt;
        this.cancellation = cancellation;
        this.roster = roster;
        this.completedAt = completedAt;
        this.completionPhotos = List.copyOf(completionPhotos);
        this.version = version;
    }

    /**
     * Makes the reservation when the checkout's payment is confirmed (RES-7): Requested with the payment held, or
     * Reserved at once under Instant (ACC-1). Nothing was held while the customer paid, so at this moment the time
     * must still be free and every booking rule must still pass, under the provider's terms, Homa's reservation
     * values and the customer's home address as they stand now, which the reservation then keeps (RES-8). The price
     * under those terms must be what was paid. Both parties' time is blocked here (RES-9, RES-10), so the first
     * confirmed payment for a time wins (RES-11). Otherwise a {@link BookingRefusedException} gives the
     * {@link CheckoutRejection} reason, nothing is blocked, and the payment is refunded in full at once.
     */
    public static Reservation request(
            ReservationId id,
            Checkout checkout,
            BookableProvider provider,
            CustomerStanding customer,
            ProviderSchedule providerSchedule,
            CustomerSchedule customerSchedule,
            Instant paidAt) {
        Booking booking = checkout.booking().at(customer.homeAddress());
        Checkout.requireBookable(booking, provider, customer, providerSchedule, customerSchedule, paidAt);
        ReservationTerms terms = provider.termsFor(booking.address());
        Quote quote = ReservationPricing.quote(terms, booking.jobTime().duration(), booking.helpers());
        if (!quote.total().equals(checkout.quote().total())) {
            throw new BookingRefusedException(BookingRefusal.PRICE_CHANGED);
        }
        Version values = ReservationValues.asOf(paidAt);
        providerSchedule.block(id, provider.calendarBlockFor(booking.jobTime(), values));
        customerSchedule.block(id, booking.jobTime().interval());
        return requested(id, checkout.id(), booking, terms, quote, values, paidAt);
    }

    /** A new reservation: Requested, or Reserved at once under Instant, which never waits for acceptance (ACC-1). */
    private static Reservation requested(ReservationId id, CheckoutId checkoutId, Booking booking,
            ReservationTerms terms, Quote quote, Version values, Instant requestedAt) {
        boolean instant = terms.acceptancePolicy().reservesAtOnce();
        return new Reservation(id, checkoutId, booking, terms, quote, values, requestedAt,
                instant ? null : AcceptanceDeadline.from(requestedAt, booking.jobTime(), values),
                instant ? ReservationStatus.RESERVED : ReservationStatus.REQUESTED, instant ? requestedAt : null,
                null, null, null, null, null, null, List.of(), 0);
    }

    /**
     * Rebuilds a reservation from what was stored, with the {@code version} it was saved at: the optimistic lock
     * that decides whether an acceptance or a withdrawal was recorded first (REQ-5). The store moves the version on
     * with each save; the reservation never does. A moment not reached yet is {@code null}, as is the acceptance
     * deadline under Instant and the roster until it is set.
     *
     * <p>Only the shape is checked: the status fits the moments it was reserved, withdrawn, declined, expired and
     * completed and whether it was cancelled; it has an acceptance deadline exactly when its terms wait for
     * acceptance; after-photos exactly once completed; a roster only once reserved; and a cancellation's settlement
     * adds up to the price that was held and stays within the fee and fine cap (CAN-2). No booking rule, notice or
     * deadline is checked again: they held when the payment was confirmed, and the reservation keeps what it was
     * Requested with (RES-8).
     */
    public static Reservation restore(ReservationId id, CheckoutId checkoutId, Booking booking, ReservationTerms terms,
            Quote quote, Version values, Instant requestedAt, AcceptanceDeadline acceptanceDeadline,
            ReservationStatus status, Instant reservedAt, Instant withdrawnAt, Instant declinedAt, Instant expiredAt,
            Cancellation cancellation, Roster roster, Instant completedAt, List<CompletionPhoto> completionPhotos,
            long version) {
        Reservation reservation = new Reservation(id, checkoutId, booking, terms, quote, values, requestedAt,
                acceptanceDeadline, status, reservedAt, withdrawnAt, declinedAt, expiredAt, cancellation, roster,
                completedAt, completionPhotos, version);
        reservation.requireStoredShape();
        return reservation;
    }

    private void requireStoredShape() {
        requireShape(passedThrough().equals(PASSED_THROUGH.get(status)), "the moments its status " + status
                + " passed through, and a status this slice reaches");
        requireShape(acceptanceDeadline().isPresent() != terms.acceptancePolicy().reservesAtOnce(),
                "an acceptance deadline exactly when its terms wait for acceptance");
        requireShape(completionPhotos.isEmpty() != completedAt().isPresent(),
                "after-photos exactly when it is completed");
        requireShape(roster().isEmpty() || reservedAt().isPresent(), "a roster only once it is reserved");
        requireShape(settlementFitsThePrice(), "a settlement that adds up to the price held, within the fee cap");
    }

    /** CAN-2: refund + fee is the amount held, and neither the fee nor the fine is more than the cap. */
    private boolean settlementFitsThePrice() {
        if (cancellation == null) {
            return true;
        }
        Settlement settlement = cancellation.settlement();
        Money cap = basis().cap();
        return settlement.customerFee().plus(settlement.customerRefund()).equals(quote.total())
                && !settlement.customerFee().isGreaterThan(cap) && !settlement.providerFine().isGreaterThan(cap);
    }

    /** The statuses the reservation has been through, read from the moments it records. */
    private Set<ReservationStatus> passedThrough() {
        Set<ReservationStatus> passed = EnumSet.noneOf(ReservationStatus.class);
        addIfReached(passed, ReservationStatus.RESERVED, reservedAt);
        addIfReached(passed, ReservationStatus.WITHDRAWN, withdrawnAt);
        addIfReached(passed, ReservationStatus.DECLINED, declinedAt);
        addIfReached(passed, ReservationStatus.EXPIRED, expiredAt);
        addIfReached(passed, ReservationStatus.COMPLETED, completedAt);
        addIfReached(passed, ReservationStatus.CANCELLED, cancellation);
        return passed;
    }

    /** A status is reached once the moment (or the cancellation) it records is there. */
    private static void addIfReached(Set<ReservationStatus> passed, ReservationStatus status, Object record) {
        if (record != null) {
            passed.add(status);
        }
    }

    private static void requireShape(boolean fits, String what) {
        if (!fits) {
            throw new IllegalArgumentException("A stored reservation needs " + what);
        }
    }

    /** ACC-2: the provider accepts a Request before the acceptance deadline, and it becomes Reserved. */
    public void accept(Instant now) {
        requireStatus(ReservationStatus.REQUESTED, "accept");
        if (!acceptanceDeadline.isOpenAt(now)) {
            throw new AcceptanceDeadlinePassedException(acceptanceDeadline.at());
        }
        status = ReservationStatus.RESERVED;
        reservedAt = now;
    }

    /**
     * REQ-2: the customer withdraws a Requested reservation before the provider accepts it, for free. It becomes
     * Withdrawn, the customer gets a full refund and the time opens again once the schedules release it. A
     * withdrawal is not a cancellation: no fee applies and it isn't counted (RTG-10). Whichever of a withdrawal and
     * an acceptance is recorded first wins (REQ-5).
     */
    public void withdraw(Instant now) {
        requireStatus(ReservationStatus.REQUESTED, "withdraw");
        status = ReservationStatus.WITHDRAWN;
        withdrawnAt = now;
    }

    /**
     * REQ-3: the provider declines a Requested reservation. It becomes Declined and the customer gets a full refund;
     * the time opens again once the schedules release it. Like a withdrawal, a decline is not a cancellation: no
     * fee, no fine and no reliability count (RTG-10). Whichever of a decline and an acceptance is recorded first wins
     * (REQ-5).
     */
    public void decline(Instant now) {
        requireStatus(ReservationStatus.REQUESTED, "decline");
        status = ReservationStatus.DECLINED;
        declinedAt = now;
    }

    /**
     * REQ-4: a request not accepted by its acceptance deadline expires. It is refused until the deadline is reached,
     * from which moment it can no longer be accepted (ACC-2), so a request is always open to one or the other. The
     * customer gets a full refund; no fee, no fine and no reliability count.
     */
    public void expire(Instant now) {
        requireStatus(ReservationStatus.REQUESTED, "expire");
        if (acceptanceDeadline.isOpenAt(now)) {
            throw new AcceptanceDeadlineNotReachedException(acceptanceDeadline.at());
        }
        status = ReservationStatus.EXPIRED;
        expiredAt = now;
    }

    /**
     * CCX-1 to CCX-6, LMC-1, LMC-2: what cancelling now would cost the customer, without cancelling, so the fee can
     * be shown before it is confirmed. The same answer {@link #cancelByCustomer} gives at this moment.
     */
    public Cancellation previewCustomerCancellation(Instant now) {
        requireCancellable(now);
        if (phaseAt(now) != ReservationPhase.BEFORE_CHECK_IN) {
            return settled(now, CancellationCause.CUSTOMER_LAST_MINUTE, CUSTOMER_LAST_MINUTE);
        }
        boolean free = new GracePeriod(reservedAt, checkInStart(), values).covers(now)
                || terms.cancellationTier().isFreeAt(now, checkInStart(), values);
        return settled(now, CancellationCause.CUSTOMER_CANCELLED, free ? FREE : LATE_CUSTOMER);
    }

    /**
     * REQ-1, CCX-1 to CCX-6, LMC-1, LMC-2: the customer cancels a Reserved reservation before its job end (a
     * Requested one is withdrawn instead). Before the Check-in Period it is free within the Grace Period or before
     * the tier's cut-off, and costs the Base Cancellation Fee after it; from the Check-in Period it costs the last
     * minute fee. The reservation becomes Cancelled, with its cause and settlement; the time opens again once the
     * schedules release it.
     */
    public Cancellation cancelByCustomer(Instant now) {
        return cancelled(previewCustomerCancellation(now));
    }

    /**
     * PCX-1, PCX-2, LMC-3: the provider cancels a Reserved reservation before its job end. Before the Check-in
     * Period the provider is fined the Base Cancellation Fee, plus 10% within 24 hours of it, and the customer is
     * refunded in full, plus 3% within 24 hours; from the Check-in Period it is fined Base + 20% and the customer
     * gets a full refund + 7%. The reservation becomes Cancelled, with its cause and settlement.
     */
    public Cancellation cancelByProvider(Instant now) {
        requireCancellable(now);
        if (phaseAt(now) != ReservationPhase.BEFORE_CHECK_IN) {
            return cancelled(settled(now, CancellationCause.PROVIDER_LAST_MINUTE, PROVIDER_LAST_MINUTE));
        }
        return cancelled(settled(now, CancellationCause.PROVIDER_CANCELLED, PROVIDER_CANCELLATION));
    }

    private void requireCancellable(Instant now) {
        requireStatus(ReservationStatus.RESERVED, "cancel");
        if (!now.isBefore(jobTime().end())) {
            throw new JobAlreadyEndedException(jobTime().end());
        }
    }

    private Cancellation settled(Instant now, CancellationCause cause, SettlementPolicy policy) {
        return new Cancellation(now, cause, policy.settle(basis(), now));
    }

    private Cancellation cancelled(Cancellation settled) {
        status = ReservationStatus.CANCELLED;
        cancellation = settled;
        return settled;
    }

    /** What the settlement policies read: the price, job time and values this reservation was Requested with (P3). */
    private SettlementBasis basis() {
        return new SettlementBasis(quote, jobTime(), values);
    }

    /**
     * ROS-1 to ROS-4, ROS-6: sets or changes the roster of a Reserved reservation before the Check-in Period starts;
     * then it is locked. Every member must be the provider's own and able to work on this job, with a verified photo
     * the customer is shown (IDN-4), and the roster must have the booked number of helpers. Supervisors are extra and not billed, so the price does not change.
     */
    public void assignRoster(List<RosterableMember> members, Instant now) {
        requireStatus(ReservationStatus.RESERVED, "roster");
        if (phaseAt(now) != ReservationPhase.BEFORE_CHECK_IN) {
            throw new RosterLockedException(checkInStart());
        }
        members.stream().filter(member -> !isRosterable(member)).findFirst().ifPresent(member -> {
            throw new MemberNotRosterableException(member.memberId());
        });
        roster = Roster.of(members.stream().map(RosterableMember::asRosterMember).toList(), booking.helpers());
    }

    /** JOB-3: the provider marks the job Completed with at least one after-photo, only after the job end time. */
    public void markCompleted(List<CompletionPhoto> photos, Instant now) {
        requireStatus(ReservationStatus.RESERVED, "mark completed");
        if (photos.isEmpty()) {
            throw new CompletionPhotoRequiredException();
        }
        if (now.isBefore(jobTime().end())) {
            throw new JobNotEndedException(jobTime().end());
        }
        status = ReservationStatus.COMPLETED;
        completedAt = now;
        completionPhotos = List.copyOf(photos);
    }

    /**
     * JOB-5: a Reserved job past its end time that isn't marked Completed. The provider is reminded at
     * {@link #completionReminders()} and the payout waits.
     */
    public boolean awaitsCompletionAt(Instant now) {
        return status == ReservationStatus.RESERVED && !now.isBefore(jobTime().end());
    }

    /** JOB-5: at the job end time and 2 hours later, under the values the reservation keeps. */
    public List<Instant> completionReminders() {
        return jobTime().completionReminders(values);
    }

    /**
     * JOB-5: the reminders due by now for a job past its end time that isn't marked Completed; none once it is
     * marked, or before the job ends.
     */
    public List<Instant> completionRemindersDueAt(Instant now) {
        if (!awaitsCompletionAt(now)) {
            return List.of();
        }
        return completionReminders().stream().filter(reminder -> !reminder.isAfter(now)).toList();
    }

    public ReservationPhase phaseAt(Instant now) {
        return jobTime().phaseAt(now, values);
    }

    /** When the Check-in Period starts, for the reservation's events. */
    public Instant checkInStart() {
        return jobTime().checkInStart(values);
    }

    /** When the Protection Window ends, for the reservation's events. */
    public Instant protectionWindowEnd() {
        return jobTime().protectionWindow(values).end();
    }

    private boolean isRosterable(RosterableMember member) {
        return member.worksFor(booking.providerId()) && member.canWorkOn(jobTime().end());
    }

    private void requireStatus(ReservationStatus expected, String act) {
        if (status != expected) {
            throw new ReservationStatusConflictException(status, act);
        }
    }

    public ReservationId id() {
        return id;
    }

    /** The checkout whose confirmed payment made this reservation. */
    public CheckoutId checkoutId() {
        return checkoutId;
    }

    /** What was booked, at the customer's home address as it was when Requested (RES-8). */
    public Booking booking() {
        return booking;
    }

    public JobTime jobTime() {
        return booking.jobTime();
    }

    public ReservationTerms terms() {
        return terms;
    }

    /** Homa's reservation values in force when the reservation was Requested, which it keeps (P3, §13). */
    public Version values() {
        return values;
    }

    /** The price; its total is the amount held from the moment the reservation is Requested (PAY-1). */
    public Quote quote() {
        return quote;
    }

    public ReservationStatus status() {
        return status;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    /** ACC-3; empty under Instant, which never waits for acceptance. */
    public Optional<AcceptanceDeadline> acceptanceDeadline() {
        return Optional.ofNullable(acceptanceDeadline);
    }

    public Optional<Instant> reservedAt() {
        return Optional.ofNullable(reservedAt);
    }

    public Optional<Instant> withdrawnAt() {
        return Optional.ofNullable(withdrawnAt);
    }

    public Optional<Instant> declinedAt() {
        return Optional.ofNullable(declinedAt);
    }

    /** When the expiry was recorded, at or after the acceptance deadline (REQ-4). */
    public Optional<Instant> expiredAt() {
        return Optional.ofNullable(expiredAt);
    }

    /** How it was cancelled, with its cause and settlement (§6); empty unless Cancelled. */
    public Optional<Cancellation> cancellation() {
        return Optional.ofNullable(cancellation);
    }

    /** ROS-6: the roster shown to the customer; empty until the provider sets it. */
    public Optional<Roster> roster() {
        return Optional.ofNullable(roster);
    }

    public Optional<Instant> completedAt() {
        return Optional.ofNullable(completedAt);
    }

    /** JOB-6: the after-photos, kept as evidence for reports. */
    public List<CompletionPhoto> completionPhotos() {
        return completionPhotos;
    }

    /**
     * The version the reservation was loaded at, 0 for one not saved yet. Whoever saves it first moves it on, and a
     * save from an older version is refused as a conflict, so of an acceptance and a withdrawal only the one recorded
     * first wins (REQ-5).
     */
    public long version() {
        return version;
    }
}
