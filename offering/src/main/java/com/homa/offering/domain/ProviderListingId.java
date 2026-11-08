package com.homa.offering.domain;

import java.util.UUID;

/** Type-safe generator for ProviderListing ids. */
public final class ProviderListingId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
