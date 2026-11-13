package reservation.usecase.exception;

public class HomaValuesNotKnownException extends RuntimeException {

    public HomaValuesNotKnownException() {
        super("Homa's values have not been announced yet");
    }
}
