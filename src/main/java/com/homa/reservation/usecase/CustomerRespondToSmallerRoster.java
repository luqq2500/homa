package com.homa.reservation.usecase;

import com.homa.kernel.Money;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.roster.RosterAdjustment;
import com.homa.reservation.usecase.dto.RespondToSmallerRosterRequest;
import com.homa.reservation.usecase.dto.RespondToSmallerRosterResponse;
import com.homa.reservation.usecase.port.in.SmallerRosterResponder;
import com.homa.reservation.usecase.port.out.ReservationIEPublisher;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import com.homa.reservation.usecase.port.out.RosterableMemberRepository;
import com.homa.reservation.usecase.port.out.TimeSource;
import java.time.Instant;

/**
 * ROS-8: the customer answers the smaller roster the provider offered, before the Check-in Period. Accepting
 * reservations the smaller team at the new price, with the booking discount kept: Payment refunds the difference at
 * once and lowers the provider's payout through {@code SettlementAdjustedIE}, and the roster change is published like
 * any other (ROS-1). Declining changes nothing about the money, tells the provider through the published event, and
 * leaves the provider to complete the roster before the Check-in Period, or ROS-7 applies when it starts. An answer
 * racing the provider's or the clock's change resolves to whichever is saved first: the loser reloads and is refused.
 */
public final class CustomerRespondToSmallerRoster implements SmallerRosterResponder {

    private final ReservationRepository reservations;
    private final RosterableMemberRepository members;
    private final ReservationIEPublisher publisher;
    private final ReservationEvents events;
    private final TimeSource time;

    public CustomerRespondToSmallerRoster(ReservationRepository reservations, RosterableMemberRepository members,
            ReservationIEPublisher publisher, ReservationEvents events, TimeSource time) {
        this.reservations = reservations;
        this.members = members;
        this.publisher = publisher;
        this.events = events;
        this.time = time;
    }

    @Override
    public RespondToSmallerRosterResponse respond(RespondToSmallerRosterRequest request) {
        return request.accept() ? accept(request) : decline(request);
    }

    private RespondToSmallerRosterResponse accept(RespondToSmallerRosterRequest request) {
        Answered answered = ScheduleRetry.retried(() -> accepted(request));
        Reservation reservation = answered.reservation();
        publisher.publish(events.rosterChanged(reservation));
        publisher.publish(events.settlementAdjusted(reservation));
        RosterAdjustment adjustment = reservation.adjustment().orElseThrow();
        return new RespondToSmallerRosterResponse(reservation.id().value(), true, reservation.helpers(),
                reservation.currentQuote().total(), adjustment.refund(), answered.at());
    }

    private RespondToSmallerRosterResponse decline(RespondToSmallerRosterRequest request) {
        Answered answered = ScheduleRetry.retried(() -> declined(request));
        Reservation reservation = answered.reservation();
        publisher.publish(events.smallerRosterDeclined(reservation, answered.at()));
        Money total = reservation.currentQuote().total();
        return new RespondToSmallerRosterResponse(reservation.id().value(), false, reservation.helpers(), total,
                Money.zero(total.currency()), answered.at());
    }

    private Answered accepted(RespondToSmallerRosterRequest request) {
        Reservation reservation = loaded(request);
        Instant now = time.now();
        reservation.acceptProposal(ReservationParties.team(members, reservation.proposedMemberIds()), now);
        reservations.save(reservation);
        return new Answered(reservation, now);
    }

    private Answered declined(RespondToSmallerRosterRequest request) {
        Reservation reservation = loaded(request);
        Instant now = time.now();
        reservation.declineProposal(now);
        reservations.save(reservation);
        return new Answered(reservation, now);
    }

    private Reservation loaded(RespondToSmallerRosterRequest request) {
        return ReservationParties.requireCustomer(
                ReservationParties.load(reservations, request.reservationId()), request.customerId());
    }

    /** What one attempt decided, once saved. */
    private record Answered(Reservation reservation, Instant at) {
    }
}
