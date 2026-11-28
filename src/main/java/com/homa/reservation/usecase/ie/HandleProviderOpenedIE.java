package com.homa.reservation.usecase.ie;

import com.homa.ie.events.ProviderOpenedIE;
import com.homa.reservation.domain.replica.ProviderReplica;
import com.homa.reservation.usecase.port.in.ProviderOpenedIEHandler;
import com.homa.reservation.usecase.port.out.BookableProviderRepository;
import com.homa.reservation.usecase.port.out.ProcessedMessageStore;

/**
 * PRV-1, PRV-11: the provider's owner, as Verification knows them. Reservations Requested from now on keep the
 * owner, who may act on every one of them. Provider's and Listing's events arrive in no set order, so this changes
 * only the owner, and a provider not heard of yet is created, not bookable, until the rest arrives.
 */
public final class HandleProviderOpenedIE implements ProviderOpenedIEHandler {

    static final String HANDLER = "reservation.ProviderOpened";

    private final ProcessedMessageStore processed;
    private final BookableProviderRepository providers;

    public HandleProviderOpenedIE(ProcessedMessageStore processed, BookableProviderRepository providers) {
        this.processed = processed;
        this.providers = providers;
    }

    @Override
    public void handle(ProviderOpenedIE event) {
        if (!processed.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        providers.save(providers.findById(event.providerId())
                .orElseGet(() -> ProviderReplica.unheardOf(event.providerId()))
                .ownedBy(event.ownerPersonId()));
    }
}
