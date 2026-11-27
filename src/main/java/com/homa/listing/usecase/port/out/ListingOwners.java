package com.homa.listing.usecase.port.out;

import com.homa.listing.domain.ProviderId;
import java.util.Optional;

/**
 * Who may create and change a provider's listing: its owner's personId, as {@code ProviderOpenedIE} named them
 * (PRV-1). Application policy, not a domain rule. Empty until that event has reached Listing.
 */
public interface ListingOwners {

    Optional<String> ownerOf(ProviderId providerId);

    /** Records the owner Provider named when the provider opened. */
    void record(ProviderId providerId, String ownerPersonId);
}
