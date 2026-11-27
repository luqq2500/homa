package com.homa.listing.usecase;

import com.homa.listing.domain.ProviderId;
import com.homa.listing.domain.ProviderStanding;
import com.homa.listing.usecase.port.out.ProviderStandingRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class InMemoryProviderStandingRepository implements ProviderStandingRepository {

    private final Map<ProviderId, ProviderStanding> stored = new HashMap<>();

    @Override
    public Optional<ProviderStanding> findById(ProviderId providerId) {
        return Optional.ofNullable(stored.get(providerId)).map(InMemoryProviderStandingRepository::copy);
    }

    @Override
    public void save(ProviderStanding standing) {
        stored.put(standing.providerId(), copy(standing));
    }

    private static ProviderStanding copy(ProviderStanding standing) {
        return ProviderStanding.rebuild(standing.providerId(), standing.live(), standing.bookable(),
                standing.activeHelperIds());
    }
}
