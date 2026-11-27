package com.homa.listing.usecase;

import com.homa.listing.domain.ProviderId;
import com.homa.listing.usecase.exception.NotAllowedException;
import com.homa.listing.usecase.exception.ProviderOwnerNotYetKnownException;
import com.homa.listing.usecase.port.out.ListingOwners;

/** Application policy: only a provider's owner creates and changes its listing (PRV-1). */
final class Ownership {

    private Ownership() {
    }

    /**
     * Refuses unless the person is the provider's owner, as {@code ProviderOpenedIE} recorded them. Before that event
     * has reached Listing the owner isn't known, so the act is refused until it has, never handed to whoever asks
     * first.
     */
    static void requireOwner(ListingOwners owners, ProviderId providerId, String personId) {
        String owner = owners.ownerOf(providerId)
                .orElseThrow(() -> new ProviderOwnerNotYetKnownException(providerId.value()));
        if (!owner.equals(personId)) {
            throw new NotAllowedException(personId + " isn't the owner of provider " + providerId.value());
        }
    }
}
