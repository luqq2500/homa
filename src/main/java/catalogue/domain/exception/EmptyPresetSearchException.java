package catalogue.domain.exception;

/** A featured search's preset search needs at least one job. */
public class EmptyPresetSearchException extends CatalogueRuleViolationException {

    public EmptyPresetSearchException() {
        super("A preset search needs at least one job");
    }
}
