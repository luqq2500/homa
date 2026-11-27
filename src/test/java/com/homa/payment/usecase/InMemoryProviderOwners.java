package com.homa.payment.usecase;

import com.homa.payment.domain.ProviderId;
import com.homa.payment.usecase.port.out.ProviderOwners;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Test only: each provider's owner, as ProviderOpenedIE recorded them. */
public class InMemoryProviderOwners implements ProviderOwners {

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
