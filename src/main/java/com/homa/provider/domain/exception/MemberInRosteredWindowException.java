package com.homa.provider.domain.exception;

/**
 * HLP-9: a member can't be removed while they're in the Check-in Period or job time of a reservation they're rostered
 * on.
 */
public final class MemberInRosteredWindowException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public MemberInRosteredWindowException(String memberId) {
        super("Team member " + memberId
                + " is in the Check-in Period or job time of a reservation they're rostered on (HLP-9)");
    }
}
