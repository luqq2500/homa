package catalogue.domain.exception;

/** A business rule of the catalogue was broken. Every catalogue domain exception is one of these. */
public abstract class CatalogueRuleViolationException extends RuntimeException {

    protected CatalogueRuleViolationException(String message) {
        super(message);
    }
}
