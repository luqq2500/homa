package com.homa.reservation.usecase;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.replica.RosterableMember;
import com.homa.reservation.domain.roster.RosterProposal;
import com.homa.reservation.usecase.dto.ProposeSmallerRosterRequest;
import com.homa.reservation.usecase.dto.ProposeSmallerRosterResponse;
import com.homa.reservation.usecase.port.in.SmallerRosterProposer;
import com.homa.reservation.usecase.port.out.ReservationIEPublisher;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import com.homa.reservation.usecase.port.out.RosterableMemberRepository;
import com.homa.reservation.usecase.port.out.TimeSource;
import java.util.List;

/**
 * ROS-8: a provider short of helpers offers the customer a smaller roster before the Check-in Period. The owner or a
 * member rostered on the reservation may offer it (PRV-11). The reservation works out the smaller team, the new price
 * with the booking discount kept and what would go back to the customer, and refuses an offer once the roster has
 * locked; the customer is told through the published event, and answers through {@link
 * com.homa.reservation.usecase.port.in.SmallerRosterResponder}.
 */
public final class ProviderProposeSmallerRoster implements SmallerRosterProposer {

    private final ReservationRepository reservations;
    private final RosterableMemberRepository members;
    private final ReservationIEPublisher publisher;
    private final ReservationEvents events;
    private final TimeSource time;

    public ProviderProposeSmallerRoster(ReservationRepository reservations, RosterableMemberRepository members,
            ReservationIEPublisher publisher, ReservationEvents events, TimeSource time) {
        this.reservations = reservations;
        this.members = members;
        this.publisher = publisher;
        this.events = events;
        this.time = time;
    }

    @Override
    public ProposeSmallerRosterResponse propose(ProposeSmallerRosterRequest request) {
        Reservation reservation = ReservationParties.requireProviderSide(
                ReservationParties.load(reservations, request.reservationId()), request.personId());
        List<RosterableMember> team = ReservationParties.team(members, request.memberIds());
        RosterProposal offer = reservation.proposeSmallerRoster(team, time.now());
        reservations.save(reservation);
        publisher.publish(events.smallerRosterProposed(reservation));
        return new ProposeSmallerRosterResponse(reservation.id().value(), offer.helpers(), offer.quote().total(),
                offer.refund(), offer.proposedAt(), reservation.checkInStart());
    }
}
