package reservation.domain.exception;

public class CompletionAlreadySubmittedException extends RuntimeException {

    public CompletionAlreadySubmittedException() {
        super("Completion is submitted; any problem goes through a report");
    }
}
