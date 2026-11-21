package com.homa.customer.domain;

import com.homa.customer.domain.exception.MissingSignInException;

/**
 * The identity-provider account a customer signs in with (CUS-7): the identity provider that confirms it, such as
 * Google or Apple ({@code issuer}), and the account's id there ({@code subject}). One sign-in holds at most one Homa
 * customer account.
 */
public record SignInId(String issuer, String subject) {

    public SignInId {
        requirePart(issuer, "an issuer");
        requirePart(subject, "a subject");
    }

    private static void requirePart(String value, String part) {
        if (value == null || value.isBlank()) {
            throw new MissingSignInException("a sign-in with " + part);
        }
    }
}
