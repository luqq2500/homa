package com.homa.listing.usecase;

import com.homa.listing.domain.ProviderId;
import com.homa.listing.usecase.exception.NotAllowedException;
import com.homa.listing.usecase.port.out.ListingOwners;
import java.util.Optional;

/** Application policy: only a provider's owner changes its listing. */
final class Ownership {

    private Ownership() {
    }

    /** Refuses unless the person is the provider's recorded owner. */
    static void requireOwner(ListingOwners owners, ProviderId providerId, String personId) {
        owners.ownerOf(providerId).filter(personId::equals).orElseThrow(() -> notOwner(providerId, personId));
    }

    /**
     * Refuses when another person is the provider's recorded owner. Before the provider goes live Listing may not
     * know its owner yet, so the person creating the listing is recorded until {@code ProviderWentLiveIE} names one.
     */
    static void requireNoOtherOwner(ListingOwners owners, ProviderId providerId, String personId) {
        Optional<String> otherOwner = owners.ownerOf(providerId).filter(owner -> !owner.equals(personId));
        if (otherOwner.isPresent()) {
            throw notOwner(providerId, personId);
        }
    }

    private static NotAllowedException notOwner(ProviderId providerId, String personId) {
        return new NotAllowedException(personId + " isn't the owner of provider " + providerId.value());
    }
}
