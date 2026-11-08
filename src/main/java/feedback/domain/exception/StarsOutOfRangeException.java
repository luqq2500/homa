package feedback.domain.exception;

/** Stars are whole numbers from 1 to 5. */
public class StarsOutOfRangeException extends FeedbackRuleViolationException {

    public StarsOutOfRangeException(int stars) {
        super("Stars must be from 1 to 5, was " + stars);
    }
}
