package com.homa.provider.domain;

import com.homa.provider.domain.exception.OwnerNotLocalException;
import com.homa.provider.domain.exception.OwnerUnderageException;
import java.util.Objects;

/**
 * The one person who runs a provider account (PRV-1): identity verified, aged 18 or above, and local, the only status
 * Malaysia's country policy allows for an owner (§14). Verification decides those facts; an owner is made only from
 * them, through {@link #verifiedAs}.
 *
 * <p>The owner can also work as a helper or supervisor in their own team (PRV-6), as a {@link TeamMember} with the
 * owner's personId, added through {@link Provider#addOwnerToTeam}. Left for slice 6: the owner's suspension suspends the
 * provider with the same cause (SUS-5).
 */
public record Owner(PersonId personId) {

    public Owner {
        Objects.requireNonNull(personId, "personId");
    }

    /**
     * The owner a verified person becomes (PRV-1).
     *
     * @throws OwnerNotLocalException when the person was verified as foreign
     * @throws OwnerUnderageException when the person did not pass the age check
     */
    public static Owner verifiedAs(VerifiedPersonFacts person) {
        if (!person.local()) {
            throw new OwnerNotLocalException(person.personId().value());
        }
        if (!person.ageCheckPassed()) {
            throw new OwnerUnderageException(person.personId().value());
        }
        return new Owner(person.personId());
    }
}
