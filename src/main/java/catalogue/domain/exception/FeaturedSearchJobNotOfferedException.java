package catalogue.domain.exception;

/** A featured search can search only for jobs providers can offer now: on the job list and not retired. */
public class FeaturedSearchJobNotOfferedException extends CatalogueRuleViolationException {

    public FeaturedSearchJobNotOfferedException(String code) {
        super("A featured search cannot search for " + code + ": it is not a job providers can offer");
    }
}
