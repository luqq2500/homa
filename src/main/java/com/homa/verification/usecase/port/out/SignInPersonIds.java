package com.homa.verification.usecase.port.out;

import com.homa.verification.domain.PersonId;
import java.util.Optional;

/**
 * The personId of each signed-in user, given on their first verification and kept for every later one, so one
 * person keeps one personId across roles (IDN-3). {@code signInId} is the user as {@code infra/security} reads them
 * from their token. Infra keeps a unique index on it, so two first verifications started at once settle on one
 * personId.
 */
public interface SignInPersonIds {

    Optional<PersonId> personIdOf(String signInId);

    void assign(String signInId, PersonId personId);
}
