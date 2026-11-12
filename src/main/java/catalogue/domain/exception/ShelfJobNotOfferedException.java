package catalogue.domain.exception;

/** A shelf can search only for jobs providers can offer now: on the job list and not retired. */
public class ShelfJobNotOfferedException extends CatalogueRuleViolationException {

    public ShelfJobNotOfferedException(String code) {
        super("A shelf cannot search for " + code + ": it is not a job providers can offer");
    }
}
