package policy.domain.exception;

/** A new version of the Homa-set values cannot take effect before the version in force. */
public class PolicyChangeBeforeCurrentVersionException extends PolicyRuleViolationException {

    public PolicyChangeBeforeCurrentVersionException(Object changedAt, Object currentFrom) {
        super("A change at " + changedAt + " cannot take effect before the version in force from " + currentFrom);
    }
}
