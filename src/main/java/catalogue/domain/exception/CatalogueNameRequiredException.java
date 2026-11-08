package catalogue.domain.exception;

/** Jobs, job groups, tools and cities each need a name. */
public class CatalogueNameRequiredException extends CatalogueRuleViolationException {

    public CatalogueNameRequiredException(String what) {
        super(what + " needs a name");
    }
}
