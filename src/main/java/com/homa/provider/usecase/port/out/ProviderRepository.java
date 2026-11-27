package com.homa.provider.usecase.port.out;

import com.homa.provider.domain.Provider;
import com.homa.provider.domain.ProviderId;
import java.util.Optional;

/** Stores providers. Infra rebuilds each one with {@link Provider#restore}. */
public interface ProviderRepository {

    Optional<Provider> findById(ProviderId id);

    /** Inserts a new provider or replaces the stored one with the same id. */
    void save(Provider provider);
}
