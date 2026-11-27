package com.homa.reservation.usecase;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.roster.Roster;
import com.homa.reservation.usecase.dto.ListMyReservationsRequest;
import com.homa.reservation.usecase.dto.ListMyReservationsResponse;
import com.homa.reservation.usecase.dto.ListMyReservationsResponse.ReservationSummary;
import com.homa.reservation.usecase.dto.ListMyReservationsResponse.RosteredMember;
import com.homa.reservation.usecase.port.in.ReservationLister;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import java.util.List;

/**
 * ROS-6: a customer or a provider lists the reservations they are a party of, earliest job first, with each rostered
 * member's name and verified photo.
 */
public final class ListMyReservations implements ReservationLister {

    private final ReservationRepository reservations;

    public ListMyReservations(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Override
    public ListMyReservationsResponse list(ListMyReservationsRequest request) {
        return new ListMyReservationsResponse(reservations.findByParty(request.partyId()).stream()
                .map(ListMyReservations::summary)
                .toList());
    }

    private static ReservationSummary summary(Reservation reservation) {
        return new ReservationSummary(reservation.id().value(), reservation.status().name(),
                reservation.booking().customerId(), reservation.booking().providerId(),
                reservation.jobTime().start(), reservation.jobTime().end(), reservation.booking().helpers(),
                reservation.quote().total(), reservation.roster().map(ListMyReservations::rostered).orElse(List.of()));
    }

    private static List<RosteredMember> rostered(Roster roster) {
        return roster.members().stream()
                .map(member -> new RosteredMember(member.memberId(), member.role().name(), member.name(),
                        member.photoReference()))
                .toList();
    }
}
