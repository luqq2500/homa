package com.homa.account.domain.exception;

/** A helper removed from the team cannot be changed. */
public class HelperRemovedException extends AccountRuleViolationException {

    public HelperRemovedException(String helperId) {
        super("Helper " + helperId + " has been removed from the team");
    }
}
