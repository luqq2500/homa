package com.homa.provider.usecase;

import com.homa.provider.domain.Provider;
import com.homa.provider.domain.ProviderId;
import com.homa.provider.usecase.port.out.ProviderRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps a copy of each saved provider and hands out a fresh copy on every read, as a database would, so a change that
 * is never saved is lost. Holds no rule.
 */
public final class InMemoryProviderRepository implements ProviderRepository {

    private final Map<ProviderId, Provider> stored = new LinkedHashMap<>();

    @Override
    public Optional<Provider> findById(ProviderId id) {
        return Optional.ofNullable(stored.get(id)).map(InMemoryProviderRepository::copyOf);
    }

    @Override
    public void save(Provider provider) {
        stored.put(provider.id(), copyOf(provider));
    }

    /** The stored provider, for assertions. */
    public Provider stored(ProviderId id) {
        return findById(id).orElseThrow();
    }

    public int count() {
        return stored.size();
    }

    private static Provider copyOf(Provider provider) {
        return Provider.restore(provider.id(), provider.owner(), provider.status(),
                provider.verifiedPayoutAccount().orElse(null));
    }
}
