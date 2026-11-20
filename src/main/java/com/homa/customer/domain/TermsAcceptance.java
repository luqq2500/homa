package com.homa.customer.domain;

import com.homa.customer.domain.exception.TermsOfServiceNotAcceptedException;
import java.time.Instant;

/** The customer's agreement to the Terms of Service, and when they gave it (CUS-2). */
public record TermsAcceptance(Instant acceptedAt) {

    public TermsAcceptance {
        if (acceptedAt == null) {
            throw new TermsOfServiceNotAcceptedException();
        }
    }

    /**
     * The customer's answer to the Terms of Service at {@code answeredAt}. Only agreeing gives an acceptance; not
     * agreeing stops the registration (CUS-2).
     */
    public static TermsAcceptance of(boolean agreed, Instant answeredAt) {
        if (!agreed) {
            throw new TermsOfServiceNotAcceptedException();
        }
        return new TermsAcceptance(answeredAt);
    }
}
