package reservation.domain.exception;

public class ReservationStatusException extends RuntimeException {

    public ReservationStatusException(String status, String action) {
        super("The reservation is " + status + ", so it cannot " + action);
    }
}
