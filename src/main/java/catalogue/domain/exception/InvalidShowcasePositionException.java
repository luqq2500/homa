package catalogue.domain.exception;

/** A place in the showcase runs from 1, the first, with no gaps. */
public class InvalidShowcasePositionException extends CatalogueRuleViolationException {

    public InvalidShowcasePositionException(int position, int last) {
        super("A place in the showcase is from 1 to " + last + ", was " + position);
    }
}
