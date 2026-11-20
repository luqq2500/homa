package com.homa.reservation.domain;

import com.homa.reservation.domain.exception.AcceptanceDeadlinePassedException;
import com.homa.reservation.domain.exception.CompletionPhotoRequiredException;
import com.homa.reservation.domain.exception.JobNotEndedException;
import com.homa.reservation.domain.exception.MemberNotRosterableException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.domain.exception.RosterLockedException;
import com.homa.reservation.domain.pricing.Quote;
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
 * A reservation: the core aggregate. It is made only when payment is secured (RES-7), keeps the terms it was made
 * with (RES-8), and moves Requested → Reserved → Completed.
 */
public final class Reservation {

    private final ReservationId id;
    private final Booking booking;
    private final ReservationTerms terms;
    private final Quote quote;
    private final Instant requestedAt;
    private final AcceptanceDeadline acceptanceDeadline;
    private ReservationStatus status;
    private Instant reservedAt;
    private Roster roster;
    private Instant completedAt;
    private List<CompletionPhoto> completionPhotos = List.of();

    private Reservation(ReservationId id, Checkout checkout, Instant paidAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.booking = checkout.booking();
        this.terms = checkout.terms();
        this.quote = checkout.quote();
        this.requestedAt = paidAt;
        boolean instant = terms.acceptancePolicy().reservesAtOnce();
        this.status = instant ? ReservationStatus.RESERVED : ReservationStatus.REQUESTED;
        this.reservedAt = instant ? paidAt : null;
        this.acceptanceDeadline = instant ? null : AcceptanceDeadline.from(paidAt, booking.jobTime());
    }

    /**
     * Makes the reservation when the checkout's payment is secured (RES-7): Requested with the payment held, or
     * Reserved at once under Instant (ACC-1). While the checkout hold is live the booking stands as checked at
     * checkout. After it expired or was released, the time must still be free and every booking rule must still
     * pass (RES-12); otherwise a {@code BookingRefusedException} says why, and the payment is refunded in full.
     * Either way the checkout's holds become the reservation's blocks (RES-9, RES-10).
     */
    public static Reservation request(
            ReservationId id,
            Checkout checkout,
            BookableProvider provider,
            CustomerStanding customer,
            ProviderSchedule providerSchedule,
            CustomerSchedule customerSchedule,
            Instant paidAt) {
        if (!providerSchedule.isHolding(checkout.id(), paidAt)) {
            Checkout.requireBookable(checkout.booking(), provider, customer, paidAt);
        }
        checkout.requireRequestableAt(paidAt);
        customerSchedule.requireFreeFor(checkout.id(), checkout.customerBlock(), paidAt);
        providerSchedule.block(checkout.id(), id, checkout.providerBlock(), paidAt);
        customerSchedule.block(checkout.id(), id, checkout.customerBlock(), paidAt);
        return new Reservation(id, checkout, paidAt);
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
     * ROS-1 to ROS-3: sets or changes the roster of a Reserved reservation before the Check-in Period starts; then
     * it is locked. Every member must be the provider's own and able to work on this job, and the roster must have
     * the booked number of helpers. Supervisors are extra and not billed, so the price does not change.
     */
    public void assignRoster(List<RosterableMember> members, Instant now) {
        requireStatus(ReservationStatus.RESERVED, "roster");
        if (phaseAt(now) != ReservationPhase.BEFORE_CHECK_IN) {
            throw new RosterLockedException(jobTime().checkInPeriod().start());
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
     * {@link JobTime#completionReminders()} and the payout waits.
     */
    public boolean awaitsCompletionAt(Instant now) {
        return status == ReservationStatus.RESERVED && !now.isBefore(jobTime().end());
    }

    public ReservationPhase phaseAt(Instant now) {
        return jobTime().phaseAt(now);
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

    public Booking booking() {
        return booking;
    }

    public JobTime jobTime() {
        return booking.jobTime();
    }

    public ReservationTerms terms() {
        return terms;
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
