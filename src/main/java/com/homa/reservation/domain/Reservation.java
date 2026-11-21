package com.homa.reservation.domain;

import com.homa.reservation.domain.ReservationValues.Version;
import com.homa.reservation.domain.exception.AcceptanceDeadlinePassedException;
import com.homa.reservation.domain.exception.BookingRefusedException;
import com.homa.reservation.domain.exception.CompletionPhotoRequiredException;
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
import com.homa.reservation.domain.terms.AcceptanceDeadline;
import com.homa.reservation.domain.terms.ReservationTerms;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A reservation: the core aggregate. It is made only when a payment is confirmed (RES-7, P1), keeps the provider's
 * terms, Homa's reservation values and the customer's address from when it was Requested (RES-8, P3), and moves
 * Requested → Reserved → Completed, or ends Withdrawn. It is never edited: a change is a new booking (P2, REQ-1).
 */
public final class Reservation {

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
    private Roster roster;
    private Instant completedAt;
    private List<CompletionPhoto> completionPhotos = List.of();

    private Reservation(ReservationId id, CheckoutId checkoutId, Booking booking, ReservationTerms terms, Quote quote,
            Version values, Instant requestedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.checkoutId = checkoutId;
        this.booking = booking;
        this.terms = terms;
        this.quote = quote;
        this.values = values;
        this.requestedAt = requestedAt;
        boolean instant = terms.acceptancePolicy().reservesAtOnce();
        this.status = instant ? ReservationStatus.RESERVED : ReservationStatus.REQUESTED;
        this.reservedAt = instant ? requestedAt : null;
        this.acceptanceDeadline = instant ? null : AcceptanceDeadline.from(requestedAt, booking.jobTime(), values);
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
        return new Reservation(id, checkout.id(), booking, terms, quote, values, paidAt);
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
     * ROS-1 to ROS-4: sets or changes the roster of a Reserved reservation before the Check-in Period starts; then
     * it is locked. Every member must be the provider's own and able to work on this job, and the roster must have
     * the booked number of helpers. Supervisors are extra and not billed, so the price does not change.
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

    /** Homa's reservation values in force when the reservation was Requested, which it keeps (P3, §11). */
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
}
