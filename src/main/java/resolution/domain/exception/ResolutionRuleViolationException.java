package resolution.domain.exception;

/** A rule of the admins' report queue was broken. Every resolution domain exception is one of these. */
public abstract class ResolutionRuleViolationException extends RuntimeException {

    protected ResolutionRuleViolationException(String message) {
        super(message);
    }
}
