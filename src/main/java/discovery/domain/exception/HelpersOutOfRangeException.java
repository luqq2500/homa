package discovery.domain.exception;

/** A search is for Homa's helpers per search. */
public class HelpersOutOfRangeException extends DiscoveryRuleViolationException {

    public HelpersOutOfRangeException(int helpers, int min, int max) {
        super(helpers + " helpers is outside Homa's range of " + min + " to " + max);
    }
}
