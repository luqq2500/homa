package com.homa.reservation.usecase;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationId;
import com.homa.reservation.usecase.exception.NotReservationPartyException;
import com.homa.reservation.usecase.exception.ReservationNotFoundException;
import com.homa.reservation.usecase.port.out.ReservationRepository;

/**
 * Who may act on a reservation. Until the provider's staff roles arrive (PRV-11), the provider acts by its own id,
 * and the customer by theirs.
 */
final class ReservationParties {

    private ReservationParties() {
    }

    static Reservation load(ReservationRepository reservations, String reservationId) {
        return reservations.findById(new ReservationId(reservationId))
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
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
