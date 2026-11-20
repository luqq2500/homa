package com.homa.provider.domain.exception;

/** HLP-1: every helper and supervisor must be aged 18 or above. */
public final class MemberUnderageException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public MemberUnderageException(String memberId) {
        super("Team member " + memberId + " is under 18 (HLP-1)");
    }
}
