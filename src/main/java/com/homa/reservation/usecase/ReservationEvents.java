package com.homa.reservation.usecase;

import com.homa.ie.events.ReservationCheckoutRejectedIE;
import com.homa.ie.events.ReservationCheckoutStartedIE;
import com.homa.ie.events.ReservationCompletedIE;
import com.homa.ie.events.ReservationDeclinedIE;
import com.homa.ie.events.ReservationExpiredIE;
import com.homa.ie.events.ReservationRequestedIE;
import com.homa.ie.events.ReservationReservedIE;
import com.homa.ie.events.ReservationWithdrawnIE;
import com.homa.ie.events.RosterChangedIE;
import com.homa.reservation.domain.Checkout;
import com.homa.reservation.domain.CheckoutRejection;
import com.homa.reservation.domain.CompletionPhoto;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.roster.RosterMember;
import com.homa.reservation.domain.terms.AcceptanceDeadline;
import com.homa.reservation.usecase.port.out.EventIds;
import com.homa.reservation.usecase.port.out.TimeSource;
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

    /** ROS-1: the roster was set or changed, and the customer is told. */
    public RosterChangedIE rosterChanged(Reservation reservation) {
        List<String> memberIds = reservation.roster().orElseThrow().members().stream()
                .map(RosterMember::memberId).toList();
        return new RosterChangedIE(eventIds.next(), reservation.id().value(), memberIds, time.now());
    }

    /** JOB-3, JOB-6: Completed, with the after-photos as evidence; the gross is informational for Payment. */
    public ReservationCompletedIE completed(Reservation reservation) {
        return new ReservationCompletedIE(eventIds.next(), reservation.id().value(),
                reservation.booking().customerId(), reservation.booking().providerId(), reservation.quote().total(),
                reservation.protectionWindowEnd(), reservation.completedAt().orElseThrow(), reservation.requestedAt(),
                reservation.completionPhotos().stream().map(CompletionPhoto::reference).toList(), time.now());
    }
}
