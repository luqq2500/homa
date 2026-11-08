package catalogue.domain.exception;

/** A retired tool cannot be retired again. */
public class ToolAlreadyRetiredException extends CatalogueRuleViolationException {

    public ToolAlreadyRetiredException(String code) {
        super("Tool " + code + " is already retired");
    }
}
