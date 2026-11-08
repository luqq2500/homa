package catalogue.domain.exception;

/** A retired job cannot be retired again. */
public class JobAlreadyRetiredException extends CatalogueRuleViolationException {

    public JobAlreadyRetiredException(String code) {
        super("Job " + code + " is already retired");
    }
}
