package reservation.domain.exception;

public class EvidenceMissingException extends RuntimeException {

    public EvidenceMissingException() {
        super("Photo or video evidence is required");
    }
}
