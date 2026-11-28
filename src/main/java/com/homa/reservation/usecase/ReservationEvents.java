package com.homa.reservation.usecase;

import com.homa.ie.events.ReservationCancelledIE;
import com.homa.ie.events.ReservationCheckoutRejectedIE;
import com.homa.ie.events.ReservationCheckoutStartedIE;
import com.homa.ie.events.ReservationCompletedIE;
import com.homa.ie.events.ReservationDeclinedIE;
import com.homa.ie.events.ReservationExpiredIE;
import com.homa.ie.events.ReservationRequestedIE;
import com.homa.ie.events.ReservationReservedIE;
import com.homa.ie.events.ReservationWithdrawnIE;
import com.homa.ie.events.RosterChangedIE;
import com.homa.ie.events.RosterNeedsReplacementIE;
import com.homa.ie.events.SettlementAdjustedIE;
import com.homa.ie.events.SmallerRosterDeclinedIE;
import com.homa.ie.events.SmallerRosterProposedIE;
import com.homa.reservation.domain.Cancellation;
import com.homa.reservation.domain.Checkout;
import com.homa.reservation.domain.CheckoutRejection;
import com.homa.reservation.domain.CompletionPhoto;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.roster.RosterAdjustment;
import com.homa.reservation.domain.roster.RosterMember;
import com.homa.reservation.domain.roster.RosterProposal;
import com.homa.reservation.domain.settlement.Settlement;
import com.homa.reservation.domain.terms.AcceptanceDeadline;
import com.homa.reservation.usecase.port.out.EventIds;
import com.homa.reservation.usecase.port.out.TimeSource;
import java.time.Instant;
import java.util.List;

/**
 * Builds the Reservation context's integration events from the saved aggregates, field by field, with a new event id
 * and the moment of publishing. Every time it carries is the reservation's own answer, never computed here.
 */
public final class ReservationEvents {

    private final EventIds eventIds;
    private final TimeSource time;

    public ReservationEvents(EventIds eventIds, TimeSource time) {
        this.eventIds = eventIds;
        this.time = time;
    }

    /** RES-11: the customer started paying; nothing is held. */
    public ReservationCheckoutStartedIE checkoutStarted(Checkout checkout) {
        return new ReservationCheckoutStartedIE(eventIds.next(), checkout.id().value(),
                checkout.booking().customerId(), checkout.booking().providerId(), checkout.quote().total(),
                time.now());
    }

    /** RES-11: the confirmed payment could not become a reservation, and is refunded in full at once. */
    public ReservationCheckoutRejectedIE checkoutRejected(Checkout checkout, CheckoutRejection reason) {
        return new ReservationCheckoutRejectedIE(eventIds.next(), checkout.id().value(),
                checkout.booking().customerId(), reason.name(), time.now());
    }

    /** RES-7: Requested; the acceptance deadline is empty under Instant (ACC-1). */
    public ReservationRequestedIE requested(Reservation reservation) {
        return new ReservationRequestedIE(eventIds.next(), reservation.id().value(),
                reservation.checkoutId().value(), reservation.booking().customerId(),
                reservation.booking().providerId(), reservation.checkInStart(), reservation.jobTime().start(),
                reservation.jobTime().end(), reservation.quote().total(),
                reservation.acceptanceDeadline().map(AcceptanceDeadline::at).orElse(null),
                reservation.protectionWindowEnd(), reservation.requestedAt(), time.now());
    }

    /** ACC-1, ACC-2: Reserved. */
    public ReservationReservedIE reserved(Reservation reservation) {
        return new ReservationReservedIE(eventIds.next(), reservation.id().value(),
                reservation.booking().customerId(), reservation.booking().providerId(), reservation.checkInStart(),
                reservation.jobTime().start(), reservation.jobTime().end(), reservation.quote().total(),
                reservation.protectionWindowEnd(), reservation.requestedAt(), time.now());
    }

    /** REQ-2: withdrawn; Payment refunds in full at once, and it never counts in reliability (RTG-10). */
    public ReservationWithdrawnIE withdrawn(Reservation reservation) {
        return new ReservationWithdrawnIE(eventIds.next(), reservation.id().value(),
                reservation.booking().customerId(), reservation.booking().providerId(),
                reservation.withdrawnAt().orElseThrow(), reservation.requestedAt(), time.now());
    }

    /** REQ-3: declined; Payment refunds in full at once. */
    public ReservationDeclinedIE declined(Reservation reservation) {
        return new ReservationDeclinedIE(eventIds.next(), reservation.id().value(),
                reservation.booking().customerId(), reservation.booking().providerId(),
                reservation.declinedAt().orElseThrow(), reservation.requestedAt(), time.now());
    }

    /** REQ-4: expired, the acceptance deadline reached; Payment refunds in full at once. */
    public ReservationExpiredIE expired(Reservation reservation) {
        return new ReservationExpiredIE(eventIds.next(), reservation.id().value(),
                reservation.booking().customerId(), reservation.booking().providerId(),
                reservation.expiredAt().orElseThrow(), reservation.requestedAt(), time.now());
    }

    /**
     * CCX, PCX, LMC: cancelled, with the settlement Reservation worked out, which Payment executes and never
     * re-derives. The settlement id is unique to the reservation, which is cancelled at most once, so a replay of the
     * event repeats the same id and Payment's idempotency keys hold.
     */
    public ReservationCancelledIE cancelled(Reservation reservation) {
        Cancellation cancellation = reservation.cancellation().orElseThrow();
        Settlement settlement = cancellation.settlement();
        return new ReservationCancelledIE(eventIds.next(), reservation.id().value(),
                reservation.booking().customerId(), reservation.booking().providerId(), cancellation.cause().name(),
                cancellation.at(), reservation.requestedAt(), reservation.checkInStart(),
                reservation.protectionWindowEnd(), "settlement-" + reservation.id().value(),
                settlement.customerFee(), settlement.customerRefund(), settlement.compensation(),
                settlement.providerFine(), settlement.providerPayoutGross(), time.now());
    }

    /** ROS-1: the roster was set or changed, and the customer is told. */
    public RosterChangedIE rosterChanged(Reservation reservation) {
        List<String> memberIds = reservation.roster().orElseThrow().members().stream()
                .map(RosterMember::memberId).toList();
        return new RosterChangedIE(eventIds.next(), reservation.id().value(), memberIds, time.now());
    }

    /** ROS-5: a rostered member stopped being active, and the provider replaces them before the Check-in Period. */
    public RosterNeedsReplacementIE rosterNeedsReplacement(Reservation reservation, String memberId) {
        return new RosterNeedsReplacementIE(eventIds.next(), reservation.id().value(),
                reservation.booking().providerId(), memberId, reservation.checkInStart(), time.now());
    }

    /** ROS-8: the provider offered a smaller roster, and the customer is told what it costs and answers by the Check-in Period. */
    public SmallerRosterProposedIE smallerRosterProposed(Reservation reservation) {
        RosterProposal offer = reservation.proposal().orElseThrow();
        return new SmallerRosterProposedIE(eventIds.next(), reservation.id().value(),
                reservation.booking().customerId(), reservation.booking().providerId(),
                offer.roster().members().stream().map(RosterMember::memberId).toList(), offer.helpers(),
                offer.quote().total(), offer.refund(), offer.proposedAt(), reservation.checkInStart(), time.now());
    }

    /** ROS-8: the customer declined it, and the provider is told it can still complete the roster before the Check-in Period. */
    public SmallerRosterDeclinedIE smallerRosterDeclined(Reservation reservation, Instant declinedAt) {
        return new SmallerRosterDeclinedIE(eventIds.next(), reservation.id().value(),
                reservation.booking().customerId(), reservation.booking().providerId(), declinedAt,
                reservation.checkInStart(), time.now());
    }

    /**
     * ROS-8: the customer accepted the smaller roster, so Payment refunds the difference at once and lowers the
     * provider's payout. The adjustment id is unique to the adjustment: each one leaves fewer helpers than the one
     * before, so a replay of the event repeats the same id and Payment's idempotency keys hold.
     */
    public SettlementAdjustedIE settlementAdjusted(Reservation reservation) {
        RosterAdjustment adjustment = reservation.adjustment().orElseThrow();
        return new SettlementAdjustedIE(eventIds.next(), reservation.id().value(),
                reservation.booking().customerId(), reservation.booking().providerId(),
                "adjustment-" + reservation.id().value() + "-" + adjustment.helpers(), "SMALLER_ROSTER",
                adjustment.refund(), adjustment.quote().total(), adjustment.adjustedAt(), time.now());
    }

    /** JOB-3, JOB-6: Completed, with the after-photos as evidence; the gross is informational for Payment. */
    public ReservationCompletedIE completed(Reservation reservation) {
        return new ReservationCompletedIE(eventIds.next(), reservation.id().value(),
                reservation.booking().customerId(), reservation.booking().providerId(), reservation.currentQuote().total(),
                reservation.protectionWindowEnd(), reservation.completedAt().orElseThrow(), reservation.requestedAt(),
                reservation.completionPhotos().stream().map(CompletionPhoto::reference).toList(), time.now());
    }
}
