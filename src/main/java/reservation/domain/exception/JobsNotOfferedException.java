package reservation.domain.exception;

public class JobsNotOfferedException extends RuntimeException {

    public JobsNotOfferedException() {
        super("A booking is for at least one job, all offered by the provider");
    }
}
