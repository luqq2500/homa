package com.homa.provider.domain.exception;

/** HLP-1: every member must be aged 18 or above, as Verification's age check decides. */
public final class MemberUnderageException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public MemberUnderageException(String memberId) {
        super("Team member " + memberId + " did not pass the age check (HLP-1)");
    }
}
