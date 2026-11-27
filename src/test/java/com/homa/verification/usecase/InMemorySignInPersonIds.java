package com.homa.verification.usecase;

import com.homa.verification.domain.PersonId;
import com.homa.verification.usecase.port.out.SignInPersonIds;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Sign-in to personId, in memory. */
public final class InMemorySignInPersonIds implements SignInPersonIds {

    private final Map<String, PersonId> bySignIn = new HashMap<>();

    @Override
    public Optional<PersonId> personIdOf(String signInId) {
        return Optional.ofNullable(bySignIn.get(signInId));
    }

    @Override
    public void assign(String signInId, PersonId personId) {
        if (bySignIn.putIfAbsent(signInId, personId) != null) {
            throw new AssertionError("A sign-in was given a second personId: " + signInId);
        }
    }
}
