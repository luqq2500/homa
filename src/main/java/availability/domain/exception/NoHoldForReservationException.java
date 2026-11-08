package availability.domain.exception;

/** Only a reservation that holds a slot can release it. */
public class NoHoldForReservationException extends AvailabilityRuleViolationException {

    public NoHoldForReservationException(String reservationId) {
        super("Reservation " + reservationId + " holds no slot");
    }
}
