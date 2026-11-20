package com.homa.provider.domain.exception;

/** HLP-3: a foreign helper or supervisor needs at least 90 days left on their work permit when verified. */
public final class WorkPermitExpiresTooSoonException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public WorkPermitExpiresTooSoonException(String memberId) {
        super("Team member " + memberId + "'s work permit expires in under 90 days (HLP-3)");
    }
}
