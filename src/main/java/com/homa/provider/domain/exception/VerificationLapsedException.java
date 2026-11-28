package com.homa.provider.domain.exception;

import java.time.Instant;

/** HLP-7: a member can be reactivated only with a valid verification, and this one fell due for re-verification. */
public final class VerificationLapsedException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public VerificationLapsedException(String memberId, Instant verifiedUntil) {
        super("Team member " + memberId + "'s verification fell due at " + verifiedUntil
                + ", so they must re-verify before they are reactivated (HLP-7)");
    }
}
