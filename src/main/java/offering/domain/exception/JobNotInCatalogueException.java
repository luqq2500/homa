package offering.domain.exception;

/** A provider can only offer jobs from Homa's job list. */
public class JobNotInCatalogueException extends OfferingRuleViolationException {

    public JobNotInCatalogueException(Object job) {
        super("Job " + job + " is not an active job on Homa's job list");
    }
}
