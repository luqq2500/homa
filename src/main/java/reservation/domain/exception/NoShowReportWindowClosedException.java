package reservation.domain.exception;

public class NoShowReportWindowClosedException extends RuntimeException {

    public NoShowReportWindowClosedException() {
        super("The no-show report window has closed");
    }
}
