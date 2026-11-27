package com.homa.payment.usecase.port.out;

import com.homa.payment.domain.ProviderId;
import java.util.Optional;

/**
 * Payment's replica of who owns each provider, fed by {@code ProviderOpenedIE} (PRV-1), so only the owner submits
 * the provider's payout account (PRV-2). Application policy, not a domain rule.
 */
public interface ProviderOwners {

    /** The owner's personId, once {@code ProviderOpenedIE} has reached Payment. */
    Optional<String> ownerOf(ProviderId providerId);

    /** Records the owner Provider named when the provider opened. */
    void record(ProviderId providerId, String ownerPersonId);
}
