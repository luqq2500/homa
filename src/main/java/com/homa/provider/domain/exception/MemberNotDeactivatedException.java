package com.homa.provider.domain.exception;

/** HLP-5: only a deactivated member can be reactivated. */
public final class MemberNotDeactivatedException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public MemberNotDeactivatedException(String memberId) {
        super("Team member " + memberId + " isn't deactivated, so there is nothing to reactivate (HLP-5)");
    }
}
