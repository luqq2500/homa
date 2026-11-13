package reservation.domain.exception;

public class ReportAlreadyFiledException extends RuntimeException {

    public ReportAlreadyFiledException() {
        super("Each party files one report per reservation");
    }
}
