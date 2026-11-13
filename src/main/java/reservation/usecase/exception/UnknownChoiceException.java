package reservation.usecase.exception;

public class UnknownChoiceException extends RuntimeException {

    public UnknownChoiceException(String choice) {
        super("Not one of Homa's options: " + choice);
    }
}
