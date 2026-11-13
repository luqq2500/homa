package offering.domain.exception;

public class NoCityCoveredException extends RuntimeException {

    public NoCityCoveredException() {
        super("A provider covers at least one city");
    }
}
