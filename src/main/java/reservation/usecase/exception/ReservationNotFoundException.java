package reservation.usecase.exception;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(String reservationId) {
        super("No reservation " + reservationId);
    }
}
