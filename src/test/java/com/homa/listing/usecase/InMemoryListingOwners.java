package com.homa.listing.usecase;

import com.homa.listing.domain.ProviderId;
import com.homa.listing.usecase.port.out.ListingOwners;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class InMemoryListingOwners implements ListingOwners {

    private final Map<ProviderId, String> owners = new HashMap<>();

    @Override
    public Optional<String> ownerOf(ProviderId providerId) {
        return Optional.ofNullable(owners.get(providerId));
    }

    @Override
    public void record(ProviderId providerId, String ownerPersonId) {
        owners.put(providerId, ownerPersonId);
    }
}
