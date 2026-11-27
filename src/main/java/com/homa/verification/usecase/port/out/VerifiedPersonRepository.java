package com.homa.verification.usecase.port.out;

import com.homa.verification.domain.IdentityNumber;
import com.homa.verification.domain.IdentityNumberStatus;
import com.homa.verification.domain.PersonId;
import com.homa.verification.domain.VerifiedPerson;
import java.util.Optional;

/**
 * Verified people, one per personId. Infra stores the identity number encrypted and keeps a unique index on it, so
 * one number recognises one person (IDN-3) even when two verifications race.
 */
public interface VerifiedPersonRepository {

    Optional<VerifiedPerson> findById(PersonId id);

    /**
     * IDN-3: who already holds {@code number}, compared as {@link IdentityNumber} compares it: {@code heldBy} that
     * person, or {@code free()}.
     */
    IdentityNumberStatus identityNumberStatusOf(IdentityNumber number);

    /** Inserts the person, or replaces the one with the same personId when they verify again. */
    void save(VerifiedPerson person);
}
