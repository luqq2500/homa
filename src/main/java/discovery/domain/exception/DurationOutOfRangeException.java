package discovery.domain.exception;

/** A search is for Homa's duration range. */
public class DurationOutOfRangeException extends DiscoveryRuleViolationException {

    public DurationOutOfRangeException(int hours, int min, int max) {
        super(hours + " hours is outside Homa's range of " + min + " to " + max);
    }
}
