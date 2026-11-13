package offering.usecase.exception;

public class HomaHoursNotKnownException extends RuntimeException {

    public HomaHoursNotKnownException() {
        super("Homa's duration range has not been announced yet");
    }
}
