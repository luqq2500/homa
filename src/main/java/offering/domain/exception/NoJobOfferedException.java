package offering.domain.exception;

public class NoJobOfferedException extends RuntimeException {

    public NoJobOfferedException() {
        super("A provider offers at least one job");
    }
}
