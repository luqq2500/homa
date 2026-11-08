package feedback.domain.exception;

/** A summary cannot withdraw more ratings than it has. */
public class NoRatingToWithdrawException extends FeedbackRuleViolationException {

    public NoRatingToWithdrawException(String subject) {
        super("There is no rating to withdraw from " + subject);
    }
}
