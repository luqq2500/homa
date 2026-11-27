package com.homa.provider.usecase.port.out;

import com.homa.provider.domain.PersonId;
import java.util.Optional;

/** The latest {@link VerifiedPerson} per personId, kept from {@code PersonVerifiedIE}. */
public interface VerifiedPersonReplica {

    Optional<VerifiedPerson> findByPersonId(PersonId personId);

    /** Inserts the person or replaces what was kept for the same personId. */
    void save(VerifiedPerson person);
}
