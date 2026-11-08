package catalogue.domain.exception;

/** A shelf's preset search needs at least one job. */
public class EmptyPresetSearchException extends CatalogueRuleViolationException {

    public EmptyPresetSearchException() {
        super("A preset search needs at least one job");
    }
}
