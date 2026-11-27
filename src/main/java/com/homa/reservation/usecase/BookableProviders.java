package com.homa.reservation.usecase;

import com.homa.reservation.domain.replica.BookableProvider;
import com.homa.reservation.usecase.exception.ProviderNotFoundException;
import com.homa.reservation.usecase.port.out.BookableProviderRepository;
import com.homa.reservation.usecase.port.out.RosterableMemberRepository;

/**
 * Loads a provider as its booking rules see it: what Reservation heard from Listing and Provider, with its active
 * helpers counted from its team (OFR-5). Shared by the use cases and handlers that book or price a provider.
 */
public final class BookableProviders {

    private final BookableProviderRepository providers;
    private final RosterableMemberRepository members;

    public BookableProviders(BookableProviderRepository providers, RosterableMemberRepository members) {
        this.providers = providers;
        this.members = members;
    }

    /** The provider, once Listing has published its listing; otherwise {@link ProviderNotFoundException}. */
    public BookableProvider find(String providerId) {
        return providers.findById(providerId)
                .flatMap(provider -> provider.asBookable(members.findByProviderId(providerId)))
                .orElseThrow(() -> new ProviderNotFoundException(providerId));
    }
}
