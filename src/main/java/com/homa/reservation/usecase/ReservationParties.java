package com.homa.reservation.usecase;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationId;
import com.homa.reservation.domain.replica.RosterableMember;
import com.homa.reservation.usecase.exception.NotReservationPartyException;
import com.homa.reservation.usecase.exception.ReservationNotFoundException;
import com.homa.reservation.usecase.exception.TeamMemberNotFoundException;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import com.homa.reservation.usecase.port.out.RosterableMemberRepository;
import java.util.List;

/**
 * Who may act on a reservation. The customer acts by their own id. On the provider's side everyone signs in with
 * their own account, and the reservation decides whether that person may act: its provider's owner, or a member
 * rostered on it (PRV-11). Only viewing and listing still go by the provider's id.
 */
final class ReservationParties {

    private ReservationParties() {
    }

    static Reservation load(ReservationRepository reservations, String reservationId) {
        return reservations.findById(new ReservationId(reservationId))
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }

    /** The provider's team members the request names, each as Reservation knows them. */
    static List<RosterableMember> team(RosterableMemberRepository members, List<String> memberIds) {
        return memberIds.stream()
                .map(memberId -> members.findById(memberId).orElseThrow(() -> new TeamMemberNotFoundException(memberId)))
                .toList();
    }

    /** Only the reservation's customer may do this. */
    static Reservation requireCustomer(Reservation reservation, String actorId) {
        if (!reservation.booking().customerId().equals(actorId)) {
            throw new NotReservationPartyException(actorId, reservation.id().value());
        }
        return reservation;
    }

    /** PRV-11: only the provider's owner, or a member rostered on this reservation, may do this for the provider. */
    static Reservation requireProviderSide(Reservation reservation, String personId) {
        if (!reservation.canBeActedOnBy(personId)) {
            throw new NotReservationPartyException(personId, reservation.id().value());
        }
        return reservation;
    }

    /** Only the reservation's provider may do this. */
    static Reservation requireProvider(Reservation reservation, String actorId) {
        if (!reservation.booking().providerId().equals(actorId)) {
            throw new NotReservationPartyException(actorId, reservation.id().value());
        }
        return reservation;
    }

    /** Only the reservation's customer or provider may do this. */
    static Reservation requireParty(Reservation reservation, String actorId) {
        if (!reservation.booking().customerId().equals(actorId)) {
            return requireProvider(reservation, actorId);
        }
        return reservation;
    }
}
