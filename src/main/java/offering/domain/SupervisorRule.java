package offering.domain;

import offering.domain.exception.InvalidSupervisorRuleException;

/**
 * When a supervisor is sent: from a minimum number of helpers, from a minimum number of hours, or
 * both. A threshold of 0 is not set; with neither set, the provider never has to send one.
 */
public record SupervisorRule(int fromHelpers, int fromHours) {

    public static final SupervisorRule NEVER = new SupervisorRule(0, 0);

    public SupervisorRule {
        if (fromHelpers < 0 || fromHours < 0) {
            throw new InvalidSupervisorRuleException();
        }
    }

    public boolean isSet() {
        return fromHelpers > 0 || fromHours > 0;
    }
}
