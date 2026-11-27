package com.homa.verification.usecase.port.out;

import com.homa.verification.domain.PersonId;
import java.util.Optional;

/**
 * Verification's replica of who owns each provider (PRV-1), so the payout-account holder check can use the owner's
 * verified identity (PRV-2, IDN-3). No published event feeds it before a provider goes live yet: see the contract
 * gap in the slice 1 report. {@code ProviderWentLiveIE} arrives only after the payout account is verified.
 */
public interface ProviderOwners {

    /** The personId of {@code providerId}'s owner, if Verification knows it. */
    Optional<PersonId> ownerOf(String providerId);
}
