package feedback.domain.exception;

/** A provider rating has stars for at least one provider category. */
public class CategoryRatingRequiredException extends FeedbackRuleViolationException {

    public CategoryRatingRequiredException() {
        super("A provider rating needs stars for at least one category");
    }
}
