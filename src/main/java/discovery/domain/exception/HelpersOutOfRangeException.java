package discovery.domain.exception;

/** A search is for at least one helper. Homa sets no upper limit. */
public class HelpersOutOfRangeException extends DiscoveryRuleViolationException {

    public HelpersOutOfRangeException(int helpers) {
        super("A search is for at least 1 helper, was " + helpers);
    }
}
