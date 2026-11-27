package com.homa.listing.usecase.port.out;

import com.homa.listing.domain.ProviderId;
import java.util.Optional;

/**
 * Who may change a provider's listing: its owner's personId. Application policy, not a domain rule. The person who
 * creates a listing is recorded first; {@code ProviderWentLiveIE} then records the owner Provider names, which
 * replaces it.
 */
public interface ListingOwners {

    Optional<String> ownerOf(ProviderId providerId);

    /** Records the owner, replacing any owner recorded before. */
    void record(ProviderId providerId, String ownerPersonId);
}
