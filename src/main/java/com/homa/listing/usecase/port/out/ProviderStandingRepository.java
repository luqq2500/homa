package com.homa.listing.usecase.port.out;

import com.homa.listing.domain.ProviderId;
import com.homa.listing.domain.ProviderStanding;
import java.util.Optional;

/** Listing's replica of each provider's standing, kept from Provider's events. */
public interface ProviderStandingRepository {

    Optional<ProviderStanding> findById(ProviderId providerId);

    void save(ProviderStanding standing);
}
