package discovery.domain.exception;

/** Codes, cities and ids cannot be left empty. */
public class DiscoveryDetailRequiredException extends DiscoveryRuleViolationException {

    public DiscoveryDetailRequiredException(String what) {
        super(what + " is required");
    }
}
