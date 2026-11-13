package reservation.domain.exception;

public class CustomerNoShowNotReportableException extends RuntimeException {

    public CustomerNoShowNotReportableException(String problem) {
        super(problem);
    }
}
