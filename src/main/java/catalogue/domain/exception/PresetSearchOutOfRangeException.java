package catalogue.domain.exception;

/** A shelf's preset search needs at least one hour and at least one helper. */
public class PresetSearchOutOfRangeException extends CatalogueRuleViolationException {

    public PresetSearchOutOfRangeException(String what, int value) {
        super("A preset search needs at least one " + what + ", was " + value);
    }
}
