package offering.domain.exception;

public class InvalidTravelFeeException extends RuntimeException {

    public InvalidTravelFeeException() {
        super("A separate travel fee is more than RM0; a provider who charges nothing includes it in the price");
    }
}
