package reservation.domain.exception;

/** Check-in needs the device within the check-in radius of the customer's location. */
public class TooFarToCheckInException extends ReservationRuleViolationException {

    public TooFarToCheckInException(int metres, int radius) {
        super("The device is " + metres + " metres away; check-in needs " + radius + " metres or less");
    }
}
