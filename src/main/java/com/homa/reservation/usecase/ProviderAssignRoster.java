package com.homa.reservation.usecase;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.replica.RosterableMember;
import com.homa.reservation.usecase.dto.AssignRosterRequest;
import com.homa.reservation.usecase.dto.AssignRosterResponse;
import com.homa.reservation.usecase.dto.AssignRosterResponse.RosteredMember;
import com.homa.reservation.usecase.exception.TeamMemberNotFoundException;
import com.homa.reservation.usecase.port.in.RosterAssigner;
import com.homa.reservation.usecase.port.out.ReservationIEPublisher;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import com.homa.reservation.usecase.port.out.RosterableMemberRepository;
import com.homa.reservation.usecase.port.out.TimeSource;
import java.util.List;

/**
 * ROS-1 to ROS-3, ROS-6: the provider sets or changes the roster of a Reserved reservation before the Check-in
 * Period, and the customer is told of every change. The reservation decides who can be rostered.
 */
public final class ProviderAssignRoster implements RosterAssigner {

    private final ReservationRepository reservations;
    private final RosterableMemberRepository members;
    private final ReservationIEPublisher publisher;
    private final ReservationEvents events;
    private final TimeSource time;

    public ProviderAssignRoster(ReservationRepository reservations, RosterableMemberRepository members,
            ReservationIEPublisher publisher, ReservationEvents events, TimeSource time) {
        this.reservations = reservations;
        this.members = members;
        this.publisher = publisher;
        this.events = events;
        this.time = time;
    }

    @Override
    public AssignRosterResponse assign(AssignRosterRequest request) {
        Reservation reservation = ReservationParties.requireProviderSide(
                ReservationParties.load(reservations, request.reservationId()), request.personId());
        List<RosterableMember> team = request.memberIds().stream()
                .map(memberId -> members.findById(memberId)
                        .orElseThrow(() -> new TeamMemberNotFoundException(memberId)))
                .toList();
        reservation.assignRoster(team, time.now());
        reservations.save(reservation);
        publisher.publish(events.rosterChanged(reservation));
        return new AssignRosterResponse(reservation.id().value(), reservation.roster().orElseThrow().members()
                .stream()
                .map(member -> new RosteredMember(member.memberId(), member.role().name(), member.name(),
                        member.photoReference()))
                .toList());
    }
}
