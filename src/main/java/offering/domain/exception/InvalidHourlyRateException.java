package offering.domain.exception;

public class InvalidHourlyRateException extends RuntimeException {

    public InvalidHourlyRateException() {
        super("The hourly rate per helper is more than RM0");
    }
}
