package com.homa.listing.usecase.port.out;

import com.homa.listing.domain.City;
import com.homa.listing.domain.ProviderId;
import com.homa.listing.domain.ProviderListing;
import java.util.List;
import java.util.Optional;

/** Providers' listings, one per provider. */
public interface ListingRepository {

    Optional<ProviderListing> findById(ProviderId providerId);

    /** Every listing whose covered cities include the city (RES-1), in any order. */
    List<ProviderListing> findCovering(City city);

    void save(ProviderListing listing);
}
