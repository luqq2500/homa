package com.homa.provider.domain.exception;

/** HLP-5: the member is already deactivated, so there is nothing to deactivate. */
public final class MemberAlreadyDeactivatedException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public MemberAlreadyDeactivatedException(String memberId) {
        super("Team member " + memberId + " is already deactivated (HLP-5)");
    }
}
