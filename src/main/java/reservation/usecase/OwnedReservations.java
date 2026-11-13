package reservation.usecase;

import reservation.domain.Reservation;
import reservation.usecase.exception.ReservationNotFoundException;
import reservation.usecase.port.out.ReservationRepository;

/** Finds a reservation for the customer or the provider it belongs to; nobody else reaches it. */
final class OwnedReservations {

    private OwnedReservations() {
    }

    static Reservation ofCustomer(ReservationRepository reservations, String customerId, String reservationId) {
        return reservations.find(reservationId)
                .filter(reservation -> reservation.customerId().equals(customerId))
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }

    static Reservation ofProvider(ReservationRepository reservations, String providerId, String reservationId) {
        return reservations.find(reservationId)
                .filter(reservation -> reservation.providerId().equals(providerId))
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }
}
