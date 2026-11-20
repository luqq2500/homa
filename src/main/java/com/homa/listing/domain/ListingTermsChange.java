package com.homa.listing.domain;

import java.util.Objects;

/**
 * What a change to a provider's listing produced: the full terms snapshot after the change, and whether the
 * minimum notice was raised automatically on switching to Request, so the provider is told (OFR-6). The usecase
 * maps it one-to-one onto {@code ListingTermsChangedIE}.
 */
public record ListingTermsChange(ListingTerms terms, boolean noticeRaised) {

    public ListingTermsChange {
        Objects.requireNonNull(terms, "terms");
    }
}
