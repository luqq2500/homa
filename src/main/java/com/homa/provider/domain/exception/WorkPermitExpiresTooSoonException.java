package com.homa.provider.domain.exception;

/** HLP-3: a foreign member needs at least 90 days left on their work permit when added. */
public final class WorkPermitExpiresTooSoonException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public WorkPermitExpiresTooSoonException(String memberId) {
        super("Team member " + memberId + "'s work permit has under 90 days left (HLP-3)");
    }
}
