package com.homa.reservation.usecase;

import com.homa.reservation.domain.CompletionPhoto;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.roster.Roster;
import com.homa.reservation.domain.terms.AcceptanceDeadline;
import com.homa.reservation.usecase.dto.ViewReservationRequest;
import com.homa.reservation.usecase.dto.ViewReservationResponse;
import com.homa.reservation.usecase.dto.ViewReservationResponse.RosteredMember;
import com.homa.reservation.usecase.port.in.ReservationViewer;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import java.util.List;

/** ROS-6: the reservation's customer or provider sees it, with each rostered member's name and verified photo. */
public final class ViewReservation implements ReservationViewer {

    private final ReservationRepository reservations;

    public ViewReservation(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Override
    public ViewReservationResponse view(ViewReservationRequest request) {
        Reservation reservation = ReservationParties.requireParty(
                ReservationParties.load(reservations, request.reservationId()), request.viewerId());
        return new ViewReservationResponse(
                reservation.id().value(),
                reservation.status().name(),
                reservation.booking().customerId(),
                reservation.booking().providerId(),
                reservation.booking().jobs().jobCodes().stream().sorted().toList(),
                reservation.checkInStart(),
                reservation.jobTime().start(),
                reservation.jobTime().end(),
                reservation.booking().helpers(),
                reservation.booking().address().addressLine(),
                reservation.booking().address().city(),
                reservation.booking().address().arrivalNote(),
                reservation.quote().hoursFee(),
                reservation.quote().travelFee(),
                reservation.quote().total(),
                reservation.requestedAt(),
                reservation.acceptanceDeadline().map(AcceptanceDeadline::at).orElse(null),
                reservation.reservedAt().orElse(null),
                reservation.completedAt().orElse(null),
                reservation.roster().map(ViewReservation::rostered).orElse(List.of()),
                reservation.completionPhotos().stream().map(CompletionPhoto::reference).toList());
    }

    private static List<RosteredMember> rostered(Roster roster) {
        return roster.members().stream()
                .map(member -> new RosteredMember(member.memberId(), member.role().name(), member.name(),
                        member.photoReference()))
                .toList();
    }
}
