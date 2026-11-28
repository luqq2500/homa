package com.homa.reservation.domain;

import com.homa.kernel.Money;
import com.homa.reservation.domain.ReservationValues.Version;
import com.homa.reservation.domain.exception.AcceptanceDeadlineNotReachedException;
import com.homa.reservation.domain.exception.AcceptanceDeadlinePassedException;
import com.homa.reservation.domain.exception.BookingRefusedException;
import com.homa.reservation.domain.exception.CheckInPeriodNotStartedException;
import com.homa.reservation.domain.exception.CompletionPhotoRequiredException;
import com.homa.reservation.domain.exception.JobAlreadyEndedException;
import com.homa.reservation.domain.exception.JobNotEndedException;
import com.homa.reservation.domain.exception.MemberNotRosterableException;
import com.homa.reservation.domain.exception.NoSmallerRosterProposedException;
import com.homa.reservation.domain.exception.NotASmallerRosterException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.domain.exception.RosterLockedException;
import com.homa.reservation.domain.exception.SmallerRosterAlreadyProposedException;
import com.homa.reservation.domain.pricing.Quote;
import com.homa.reservation.domain.pricing.ReservationPricing;
import com.homa.reservation.domain.replica.BookableProvider;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.domain.replica.RosterableMember;
import com.homa.reservation.domain.roster.MemberRole;
import com.homa.reservation.domain.roster.Roster;
import com.homa.reservation.domain.roster.RosterAdjustment;
import com.homa.reservation.domain.roster.RosterMember;
import com.homa.reservation.domain.roster.RosterProposal;
import com.homa.reservation.domain.schedule.CustomerSchedule;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import com.homa.reservation.domain.settlement.CustomerLastMinutePolicy;
import com.homa.reservation.domain.settlement.FreeCancellationPolicy;
import com.homa.reservation.domain.settlement.LateCustomerCancellationPolicy;
import com.homa.reservation.domain.settlement.NoRosterPolicy;
import com.homa.reservation.domain.settlement.ProviderCancellationPolicy;
import com.homa.reservation.domain.settlement.ProviderLastMinutePolicy;
import com.homa.reservation.domain.settlement.Settlement;
import com.homa.reservation.domain.settlement.SettlementBasis;
import com.homa.reservation.domain.settlement.SettlementPolicy;
import com.homa.reservation.domain.settlement.SmallerRosterPolicy;
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
import java.util.stream.Collectors;

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
    private static final SettlementPolicy NO_ROSTER = new NoRosterPolicy();

    private final ReservationId id;
    private final CheckoutId checkoutId;
    private final String ownerPersonId;
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
    private RosterProposal proposal;
    private RosterAdjustment adjustment;
    private Instant completedAt;
    private List<CompletionPhoto> completionPhotos;
    private final long version;

    private Reservation(ReservationId id, CheckoutId checkoutId, String ownerPersonId, Booking booking,
            ReservationTerms terms, Quote quote, Version values, Instant requestedAt,
            AcceptanceDeadline acceptanceDeadline, ReservationStatus status, Instant reservedAt, Instant withdrawnAt,
            Instant declinedAt, Instant expiredAt, Cancellation cancellation, Roster roster, RosterProposal proposal,
            RosterAdjustment adjustment, Instant completedAt, List<CompletionPhoto> completionPhotos, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.checkoutId = Objects.requireNonNull(checkoutId, "checkoutId");
        this.ownerPersonId = Objects.requireNonNull(ownerPersonId, "ownerPersonId");
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
        this.proposal = proposal;
        this.adjustment = adjustment;
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
        return requested(id, checkout.id(), provider.ownerPersonId(), booking, terms, quote, values, paidAt);
    }

    /** A new reservation: Requested, or Reserved at once under Instant, which never waits for acceptance (ACC-1). */
    private static Reservation requested(ReservationId id, CheckoutId checkoutId, String ownerPersonId,
            Booking booking, ReservationTerms terms, Quote quote, Version values, Instant requestedAt) {
        boolean instant = terms.acceptancePolicy().reservesAtOnce();
        return new Reservation(id, checkoutId, ownerPersonId, booking, terms, quote, values, requestedAt,
                instant ? null : AcceptanceDeadline.from(requestedAt, booking.jobTime(), values),
                instant ? ReservationStatus.RESERVED : ReservationStatus.REQUESTED, instant ? requestedAt : null,
                null, null, null, null, null, null, null, null, List.of(), 0);
    }

    /**
     * Rebuilds a reservation from what was stored, with the {@code version} it was saved at: the optimistic lock
     * that decides whether an acceptance or a withdrawal was recorded first (REQ-5). The store moves the version on
     * with each save; the reservation never does. A moment not reached yet is {@code null}, as is the acceptance
     * deadline under Instant and the roster until it is set.
     *
     * <p>Only the shape is checked: the status fits the moments it was reserved, withdrawn, declined, expired and
     * completed and whether it was cancelled; it has an acceptance deadline exactly when its terms wait for
     * acceptance; after-photos exactly once completed; a roster, an offered smaller roster and an accepted one only
     * once reserved; an accepted smaller roster with fewer helpers at a lower price, an offered one with fewer helpers
     * than there are now, and a roster with the helpers the reservation has now; and a cancellation's settlement adds
     * up to the price now held and stays within the fee and fine cap (CAN-2). No booking rule, notice or
     * deadline is checked again: they held when the payment was confirmed, and the reservation keeps what it was
     * Requested with (RES-8).
     */
    public static Reservation restore(ReservationId id, CheckoutId checkoutId, String ownerPersonId, Booking booking,
            ReservationTerms terms, Quote quote, Version values, Instant requestedAt,
            AcceptanceDeadline acceptanceDeadline, ReservationStatus status, Instant reservedAt, Instant withdrawnAt,
            Instant declinedAt, Instant expiredAt, Cancellation cancellation, Roster roster, RosterProposal proposal,
            RosterAdjustment adjustment, Instant completedAt, List<CompletionPhoto> completionPhotos, long version) {
        Reservation reservation = new Reservation(id, checkoutId, ownerPersonId, booking, terms, quote, values,
                requestedAt, acceptanceDeadline, status, reservedAt, withdrawnAt, declinedAt, expiredAt, cancellation,
                roster, proposal, adjustment, completedAt, completionPhotos, version);
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
        requireShape(rosterHasTheHelpersItHas(), "a roster with the helpers the reservation has");
        requireShape(offerFits(), "a smaller roster offered only once reserved, with fewer helpers than it has");
        requireShape(adjustmentFits(), "a smaller roster accepted only once reserved, with fewer helpers at a lower price");
        requireShape(settlementFitsThePrice(), "a settlement that adds up to the price held, within the fee cap");
    }

    private boolean rosterHasTheHelpersItHas() {
        return roster == null || roster.helpers().size() == helpers();
    }

    private boolean offerFits() {
        return proposal == null || (reservedAt != null && proposal.helpers() < helpers());
    }

    private boolean adjustmentFits() {
        return adjustment == null || (reservedAt != null && adjustment.helpers() < booking.helpers()
                && adjustment.quote().total().isLessThan(quote.total()));
    }

    /** CAN-2: refund + fee is the amount held, and neither the fee nor the fine is more than the cap. */
    private boolean settlementFitsThePrice() {
        if (cancellation == null) {
            return true;
        }
        Settlement settlement = cancellation.settlement();
        Money cap = basis().cap();
        return settlement.customerFee().plus(settlement.customerRefund()).equals(currentQuote().total())
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
        proposal = null;
        return settled;
    }

    /**
     * What the settlement policies read: the price now, job time and values this reservation was Requested with (P3).
     * The price now is the booked one, or the smaller roster's once the customer accepted it (ROS-8).
     */
    private SettlementBasis basis() {
        return new SettlementBasis(currentQuote(), jobTime(), values);
    }

    /**
     * ROS-1 to ROS-4, ROS-6: sets or changes the roster of a Reserved reservation before the Check-in Period starts;
     * then it is locked. Every member must be the provider's own and able to work on this job, with a verified photo
     * the customer is shown (IDN-4), and the roster must have the booked number of helpers. Supervisors are extra and not billed, so the price does not change.
     */
    public void assignRoster(List<RosterableMember> members, Instant now) {
        requireRosterOpenAt(now, "roster");
        requireRosterable(members);
        roster = Roster.of(members.stream().map(RosterableMember::asRosterMember).toList(), helpers());
        proposal = null;
    }

    /**
     * ROS-8: a provider short of helpers offers a smaller roster before the Check-in Period starts, and the customer
     * answers it. The offer is worked out now: the smaller team the customer is shown, the booked job re-quoted for
     * fewer helpers with the booking discount kept, and what would go back to them. Every member must be the
     * provider's own and able to work on this job, and the helpers must be at least one and fewer than the
     * reservation has now. One offer waits for an answer at a time; setting a complete roster, a decline or the
     * Check-in Period ends it.
     */
    public RosterProposal proposeSmallerRoster(List<RosterableMember> members, Instant now) {
        requireRosterOpenAt(now, "offer a smaller roster on");
        if (proposal != null) {
            throw new SmallerRosterAlreadyProposedException();
        }
        requireRosterable(members);
        int offered = (int) members.stream().filter(member -> member.role() == MemberRole.HELPER).count();
        requireSmaller(offered);
        SmallerRosterPolicy policy = new SmallerRosterPolicy(terms, offered);
        proposal = new RosterProposal(
                Roster.of(members.stream().map(RosterableMember::asRosterMember).toList(), offered),
                policy.requote(basis()), policy.settle(basis(), now), now);
        return proposal;
    }

    private void requireSmaller(int offeredHelpers) {
        if (offeredHelpers < 1 || offeredHelpers >= helpers()) {
            throw new NotASmallerRosterException(offeredHelpers, helpers());
        }
    }

    /**
     * ROS-8: the customer accepts the smaller roster before the Check-in Period starts. The reservation goes on with
     * the smaller team and the new price, which keeps the booking discount; the difference is refunded and the
     * provider isn't fined. {@code team} is the offered members as they are now, and each must still be able to work
     * on this job, or the offer can't be accepted and the provider makes a new one.
     */
    public RosterAdjustment acceptProposal(List<RosterableMember> team, Instant now) {
        RosterProposal accepted = requireProposalOpenAt(now, "accept a smaller roster of");
        requireOfferedMembersRosterable(accepted, team);
        adjustment = new RosterAdjustment(accepted.helpers(), accepted.quote(), accepted.settlement(), now);
        roster = accepted.roster();
        proposal = null;
        return adjustment;
    }

    /**
     * ROS-8: the customer declines the smaller roster before the Check-in Period starts. The reservation stays as it
     * was booked: the provider can still set a complete roster before the Check-in Period, and if it doesn't, ROS-7
     * applies when that starts.
     */
    public void declineProposal(Instant now) {
        requireProposalOpenAt(now, "decline a smaller roster of");
        proposal = null;
    }

    private RosterProposal requireProposalOpenAt(Instant now, String act) {
        requireRosterOpenAt(now, act);
        if (proposal == null) {
            throw new NoSmallerRosterProposedException();
        }
        return proposal;
    }

    private void requireOfferedMembersRosterable(RosterProposal offered, List<RosterableMember> team) {
        Set<String> able = team.stream().filter(this::isRosterable).map(RosterableMember::memberId)
                .collect(Collectors.toSet());
        offered.roster().members().stream().map(RosterMember::memberId).filter(memberId -> !able.contains(memberId))
                .findFirst().ifPresent(memberId -> {
                    throw new MemberNotRosterableException(memberId);
                });
    }

    /** ROS-1: the roster, and an offer of a smaller one, change only while the Check-in Period hasn't started. */
    private void requireRosterOpenAt(Instant now, String act) {
        requireStatus(ReservationStatus.RESERVED, act);
        if (phaseAt(now) != ReservationPhase.BEFORE_CHECK_IN) {
            throw new RosterLockedException(checkInStart());
        }
    }

    private void requireRosterable(List<RosterableMember> members) {
        members.stream().filter(member -> !isRosterable(member)).findFirst().ifPresent(member -> {
            throw new MemberNotRosterableException(member.memberId());
        });
    }

    /** The ids of the members of the smaller roster on offer; none when no offer is waiting for an answer. */
    public List<String> proposedMemberIds() {
        if (proposal == null) {
            return List.of();
        }
        return proposal.roster().members().stream().map(RosterMember::memberId).toList();
    }

    /**
     * ROS-1, ROS-7, NRS-1 to NRS-3: the Check-in Period has started, so the roster locks as it is. A complete roster
     * is locked and the reservation goes on. A roster that isn't complete, because none was set or a member who
     * stopped being active wasn't replaced (ROS-5), triggers the No Roster Policy: the reservation is Cancelled with
     * cause NO_ROSTER, the provider is fined the Base Cancellation Fee + 12% and the customer gets a full refund + 4%.
     * It is refused before the Check-in Period starts, once the job has ended, and for a roster already locked, so a
     * repeat changes nothing. Returns the cancellation when there was no complete roster.
     */
    public Optional<Cancellation> lockRosterAtCheckIn(Instant now) {
        requireRosterToLockAt(now);
        if (hasCompleteRoster()) {
            roster = roster.locked(now);
            proposal = null;
            return Optional.empty();
        }
        return Optional.of(cancelled(settled(now, CancellationCause.NO_ROSTER, NO_ROSTER)));
    }

    private void requireRosterToLockAt(Instant now) {
        requireStatus(ReservationStatus.RESERVED, "lock the roster of");
        if (phaseAt(now) == ReservationPhase.BEFORE_CHECK_IN) {
            throw new CheckInPeriodNotStartedException(checkInStart());
        }
        if (!now.isBefore(jobTime().end())) {
            throw new JobAlreadyEndedException(jobTime().end());
        }
        if (isRosterLocked()) {
            throw new RosterLockedException(checkInStart());
        }
    }

    private boolean hasCompleteRoster() {
        return roster != null && roster.isComplete();
    }

    private boolean isRosterLocked() {
        return roster != null && roster.isLocked();
    }

    /**
     * ROS-5: a rostered member stopped being active (deactivated, removed or suspended). Before the Check-in Period
     * the provider replaces them, so the roster is flagged and isn't complete until it is set again (ROS-7). From the
     * Check-in Period the roster is locked and nothing changes here (ROS-9, ROS-10 cover a member lost later).
     * Returns whether this flagged the roster: false when the reservation is no longer open to roster changes, the
     * member isn't on its roster, or they were flagged already.
     */
    public boolean rosterMemberStoppedBeingActive(String memberId, Instant now) {
        if (roster == null || !rosterCanStillChangeAt(now) || !roster.canFlagForReplacement(memberId)) {
            return false;
        }
        roster = roster.flaggedForReplacement(memberId);
        return true;
    }

    /** ROS-1: a Reserved reservation's roster changes only until the Check-in Period starts. */
    private boolean rosterCanStillChangeAt(Instant now) {
        return status == ReservationStatus.RESERVED && phaseAt(now) == ReservationPhase.BEFORE_CHECK_IN;
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

    /**
     * PRV-11: everyone signs in with their own account, and a provider-side action on this reservation comes from
     * the provider's owner or from a member rostered on it, whichever of them is signed in. The owner acts on every
     * reservation of their provider; a member only on the ones they are rostered on, and not once someone else has
     * replaced them. Security only maps the token to a personId; this decides.
     */
    public boolean canBeActedOnBy(String personId) {
        return ownerPersonId.equals(personId)
                || roster().map(rostered -> rostered.includesPerson(personId)).orElse(false);
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

    /** The owner who runs the provider, as they were when the reservation was Requested (PRV-1, PRV-11). */
    public String ownerPersonId() {
        return ownerPersonId;
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

    /**
     * The price it was booked at; its total is the amount held from the moment the reservation is Requested (PAY-1).
     * It never changes (P3); see {@link #currentQuote()} for the price after a smaller roster.
     */
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

    /**
     * The helpers the reservation has: the booked number, or the smaller roster's once the customer accepted it
     * (ROS-8).
     */
    public int helpers() {
        return adjustment == null ? booking.helpers() : adjustment.helpers();
    }

    /**
     * The price now, which every later settlement works from: the booked {@link #quote()}, or the smaller roster's
     * once the customer accepted it, with the booking discount kept (ROS-8).
     */
    public Quote currentQuote() {
        return adjustment == null ? quote : adjustment.quote();
    }

    /** ROS-8: the smaller roster the provider offered and the customer hasn't answered; empty when none waits. */
    public Optional<RosterProposal> proposal() {
        return Optional.ofNullable(proposal);
    }

    /** ROS-8: the smaller roster the customer accepted, the latest one if there were several; empty otherwise. */
    public Optional<RosterAdjustment> adjustment() {
        return Optional.ofNullable(adjustment);
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
