package com.homa.reservation.domain.replica;

import java.util.Objects;
import java.util.Optional;

/**
 * The verified face photo of a person, as Verification checks it against their identity (IDN-4). A new photo
 * replaces the one shown only once it is verified; until then the old verified photo stays.
 *
 * @param verifiedReference the person's verified photo in the photo store; empty until one is verified
 */
public record PersonPhoto(String personId, Optional<String> verifiedReference) {

    public PersonPhoto {
        Objects.requireNonNull(personId, "personId");
        Objects.requireNonNull(verifiedReference, "verifiedReference");
    }

    /** A person with no verified photo yet. */
    public static PersonPhoto none(String personId) {
        return new PersonPhoto(personId, Optional.empty());
    }

    /** IDN-4: a checked photo becomes the person's photo only if it was verified; otherwise the old one stays. */
    public PersonPhoto checked(boolean verified, String reference) {
        return verified ? new PersonPhoto(personId, Optional.of(reference)) : this;
    }
}
