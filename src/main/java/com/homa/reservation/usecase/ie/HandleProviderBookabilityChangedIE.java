package com.homa.reservation.usecase.ie;

import com.homa.ie.events.ProviderBookabilityChangedIE;
import com.homa.reservation.domain.replica.ProviderReplica;
import com.homa.reservation.usecase.port.in.ProviderBookabilityChangedIEHandler;
import com.homa.reservation.usecase.port.out.BookableProviderRepository;
import com.homa.reservation.usecase.port.out.ProcessedMessageStore;

/**
 * PRV-2: the provider became bookable again or stopped being bookable. New bookings follow it; booked reservations
 * stay as they are.
 */
public final class HandleProviderBookabilityChangedIE implements ProviderBookabilityChangedIEHandler {

    static final String HANDLER = "reservation.ProviderBookabilityChanged";

    private final ProcessedMessageStore processed;
    private final BookableProviderRepository providers;

    public HandleProviderBookabilityChangedIE(ProcessedMessageStore processed, BookableProviderRepository providers) {
        this.processed = processed;
        this.providers = providers;
    }

    @Override
    public void handle(ProviderBookabilityChangedIE event) {
        if (!processed.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        providers.save(providers.findById(event.providerId())
                .orElseGet(() -> ProviderReplica.unheardOf(event.providerId()))
                .withBookability(event.bookable()));
    }
}
