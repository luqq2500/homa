package com.homa.provider.domain.exception;

/**
 * HLP-8: a removed member can only come back as a new member, verified again, so nothing more can be done to the one
 * who was removed.
 */
public final class MemberRemovedException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public MemberRemovedException(String memberId) {
        super("Team member " + memberId + " was removed and can only come back as a new member (HLP-8)");
    }
}
