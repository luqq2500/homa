package com.homa.reservation.usecase.ie;

import com.homa.ie.events.ProviderWentLiveIE;
import com.homa.reservation.domain.replica.ProviderReplica;
import com.homa.reservation.usecase.port.in.ProviderWentLiveIEHandler;
import com.homa.reservation.usecase.port.out.BookableProviderRepository;
import com.homa.reservation.usecase.port.out.ProcessedMessageStore;

/** PRV-1, PRV-2: the provider went live, so it can be booked once Listing has published its listing. */
public final class HandleProviderWentLiveIE implements ProviderWentLiveIEHandler {

    static final String HANDLER = "reservation.ProviderWentLive";

    private final ProcessedMessageStore processed;
    private final BookableProviderRepository providers;

    public HandleProviderWentLiveIE(ProcessedMessageStore processed, BookableProviderRepository providers) {
        this.processed = processed;
        this.providers = providers;
    }

    @Override
    public void handle(ProviderWentLiveIE event) {
        if (!processed.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        providers.save(providers.findById(event.providerId())
                .orElseGet(() -> ProviderReplica.unheardOf(event.providerId()))
                .withBookability(true));
    }
}
