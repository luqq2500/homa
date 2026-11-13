package reservation.domain.exception;

public class ReportWindowClosedException extends RuntimeException {

    public ReportWindowClosedException() {
        super("Reports open at the booked start and close when the dispute window ends");
    }
}
