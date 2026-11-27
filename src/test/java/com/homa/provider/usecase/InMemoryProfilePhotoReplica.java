package com.homa.provider.usecase;

import com.homa.provider.domain.PersonId;
import com.homa.provider.usecase.port.out.ProfilePhotoReplica;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** The profile-photo replica in a map. Holds no rule. */
public final class InMemoryProfilePhotoReplica implements ProfilePhotoReplica {

    private final Map<PersonId, Boolean> photos = new HashMap<>();

    @Override
    public Optional<Boolean> photoVerifiedOf(PersonId personId) {
        return Optional.ofNullable(photos.get(personId));
    }

    @Override
    public void record(PersonId personId, boolean photoVerified) {
        photos.put(personId, photoVerified);
    }
}
