package com.homa.verification.usecase.port.out;

import com.homa.verification.domain.PersonId;
import java.util.Optional;

/**
 * Verification's replica of who owns each provider (PRV-1), fed by {@code ProviderOpenedIE}, so the payout-account
 * holder check can use the owner's verified identity (PRV-2, IDN-3).
 */
public interface ProviderOwners {

    /** The personId of {@code providerId}'s owner, once {@code ProviderOpenedIE} has reached Verification. */
    Optional<PersonId> ownerOf(String providerId);

    /** Records the owner Provider named when the provider opened. */
    void record(String providerId, PersonId ownerPersonId);
}
