package policy.domain.exception;

/** The response deadline is shorter than the minimum notice, so a request is never made after its own deadline. */
public class ResponseDeadlineNotBeforeMinimumNoticeException extends PolicyRuleViolationException {

    public ResponseDeadlineNotBeforeMinimumNoticeException(Object responseDeadline, Object minimumNotice) {
        super("The response deadline of " + responseDeadline + " must be shorter than the minimum notice of " + minimumNotice);
    }
}
