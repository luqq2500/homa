package com.homa.verification.usecase;

import com.homa.verification.domain.exception.DocumentNotAcceptedException;
import com.homa.verification.domain.exception.DocumentNotSetForStatusException;
import com.homa.verification.domain.exception.IdentityNotVerifiedException;
import com.homa.verification.domain.exception.IdentityNumberAlreadyHeldException;
import com.homa.verification.domain.exception.MissingIdentityDetailException;
import com.homa.verification.domain.exception.PersonUnderageException;
import com.homa.verification.domain.exception.StatusNotAllowedForRoleException;
import com.homa.verification.domain.exception.VerificationRuleViolationException;
import com.homa.verification.domain.exception.WorkPermitExpiryMissingException;
import java.util.Map;

/**
 * The reason {@code PersonVerificationRefusedIE} gives for each refusal, in the contract's words. Other contexts read
 * the reason, never the rule's message, which is Verification's own.
 */
final class RefusalReasons {

    static final String UNDER_MINIMUM_AGE = "UNDER_MINIMUM_AGE";
    static final String STATUS_NOT_ALLOWED_FOR_ROLE = "STATUS_NOT_ALLOWED_FOR_ROLE";
    static final String IDENTITY_ALREADY_HELD = "IDENTITY_ALREADY_HELD";
    static final String DOCUMENT_NOT_ACCEPTED = "DOCUMENT_NOT_ACCEPTED";
    static final String CHECK_NOT_PASSED = "CHECK_NOT_PASSED";

    /**
     * CUS-10, PRV-1 and HLP-1 give the age and status refusals, IDN-3 the identity held by another person. A document
     * that isn't the one the policy names, or that doesn't give a fact Homa needs (a name, a number or a permit
     * expiry), isn't accepted (IDN-1, IDN-2, HLP-3).
     */
    private static final Map<Class<? extends VerificationRuleViolationException>, String> BY_REFUSAL = Map.of(
            PersonUnderageException.class, UNDER_MINIMUM_AGE,
            StatusNotAllowedForRoleException.class, STATUS_NOT_ALLOWED_FOR_ROLE,
            IdentityNumberAlreadyHeldException.class, IDENTITY_ALREADY_HELD,
            DocumentNotAcceptedException.class, DOCUMENT_NOT_ACCEPTED,
            DocumentNotSetForStatusException.class, DOCUMENT_NOT_ACCEPTED,
            WorkPermitExpiryMissingException.class, DOCUMENT_NOT_ACCEPTED,
            MissingIdentityDetailException.class, DOCUMENT_NOT_ACCEPTED,
            IdentityNotVerifiedException.class, CHECK_NOT_PASSED);

    private RefusalReasons() {
    }

    /** The reason for {@code refusal}; any refusal not listed is the check not passing. */
    static String of(VerificationRuleViolationException refusal) {
        return BY_REFUSAL.getOrDefault(refusal.getClass(), CHECK_NOT_PASSED);
    }
}
