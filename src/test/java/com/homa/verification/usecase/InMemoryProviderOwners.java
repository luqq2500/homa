package com.homa.verification.usecase;

import com.homa.verification.domain.PersonId;
import com.homa.verification.usecase.port.out.ProviderOwners;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Provider to owner's personId, in memory. */
public final class InMemoryProviderOwners implements ProviderOwners {

    private final Map<String, PersonId> owners = new HashMap<>();

    @Override
    public Optional<PersonId> ownerOf(String providerId) {
        return Optional.ofNullable(owners.get(providerId));
    }

    public void ownedBy(String providerId, String personId) {
        owners.put(providerId, new PersonId(personId));
    }
}
