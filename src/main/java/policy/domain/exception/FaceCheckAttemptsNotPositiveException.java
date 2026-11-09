package policy.domain.exception;

/** Each person gets at least one face check attempt. */
public class FaceCheckAttemptsNotPositiveException extends PolicyRuleViolationException {

    public FaceCheckAttemptsNotPositiveException(int attempts) {
        super("The face check attempts must be at least 1, was " + attempts);
    }
}
