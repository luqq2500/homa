package offering.domain.exception;

public class JobNotOfferableException extends RuntimeException {

    public JobNotOfferableException(String jobId) {
        super("The job " + jobId + " is not on Homa's job list");
    }
}
