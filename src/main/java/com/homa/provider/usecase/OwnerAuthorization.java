package com.homa.provider.usecase;

import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.ProviderId;
import com.homa.provider.usecase.exception.NotAllowedException;
import com.homa.provider.usecase.exception.ProviderNotFoundException;
import com.homa.provider.usecase.port.out.ProviderRepository;

/** Who may act on a provider's account: only its owner, the one person who runs it (PRV-1). */
final class OwnerAuthorization {

    private OwnerAuthorization() {
    }

    /**
     * The provider, when {@code actingPersonId} runs it.
     *
     * @throws ProviderNotFoundException when no provider has the id
     * @throws NotAllowedException       when the acting person isn't its owner
     */
    static Provider providerRunBy(ProviderRepository providers, String providerId, String actingPersonId) {
        Provider provider = providers.findById(new ProviderId(providerId))
                .orElseThrow(() -> new ProviderNotFoundException(providerId));
        if (!provider.isRunBy(new PersonId(actingPersonId))) {
            throw new NotAllowedException(actingPersonId, providerId);
        }
        return provider;
    }
}
