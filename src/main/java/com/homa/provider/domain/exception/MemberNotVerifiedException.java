package com.homa.provider.domain.exception;

/**
 * HLP-2, HLP-3: a citizen must be verified as a citizen, and a foreign worker as legally allowed to work in the
 * country, before they can be added.
 */
public final class MemberNotVerifiedException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public MemberNotVerifiedException(String memberId) {
        super("Team member " + memberId + " did not pass identity verification (HLP-2, HLP-3)");
    }
}
