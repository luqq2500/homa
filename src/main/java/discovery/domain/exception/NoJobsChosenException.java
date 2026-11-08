package discovery.domain.exception;

/** A search has at least one job. */
public class NoJobsChosenException extends DiscoveryRuleViolationException {

    public NoJobsChosenException() {
        super("A search needs at least one job");
    }
}
