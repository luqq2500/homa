package catalogue.domain.exception;

/** A shelf's place on the homepage runs from 1, the first shelf, with no gaps. */
public class InvalidShelfOrderException extends CatalogueRuleViolationException {

    public InvalidShelfOrderException(int position, int last) {
        super("A shelf's place is from 1 to " + last + ", was " + position);
    }
}
