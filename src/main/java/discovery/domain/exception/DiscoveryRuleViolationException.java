package discovery.domain.exception;

/** A search rule was broken. Every discovery domain exception is one of these. */
public abstract class DiscoveryRuleViolationException extends RuntimeException {

    protected DiscoveryRuleViolationException(String message) {
        super(message);
    }
}
