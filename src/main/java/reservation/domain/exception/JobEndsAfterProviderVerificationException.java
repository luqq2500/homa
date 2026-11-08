package reservation.domain.exception;

/**
 * A provider is requested and accepts only for a job that ends by their re-verification due date, so
 * they are verified for the whole job.
 */
public class JobEndsAfterProviderVerificationException extends ReservationRuleViolationException {

    public JobEndsAfterProviderVerificationException(Object jobEnd, Object verifiedUntil) {
        super("The job ends at " + jobEnd + ", after the provider's re-verification is due at " + verifiedUntil);
    }
}
