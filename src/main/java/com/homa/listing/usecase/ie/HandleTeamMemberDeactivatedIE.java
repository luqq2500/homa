package com.homa.listing.usecase.ie;

import com.homa.ie.events.TeamMemberDeactivatedIE;
import com.homa.listing.domain.ListingTermsChange;
import com.homa.listing.domain.ProviderId;
import com.homa.listing.domain.ProviderStanding;
import com.homa.listing.usecase.ListingEvents;
import com.homa.listing.usecase.port.in.TeamMemberDeactivatedIEHandler;
import com.homa.listing.usecase.port.out.ListingIEPublisher;
import com.homa.listing.usecase.port.out.ListingRepository;
import com.homa.listing.usecase.port.out.ProcessedMessageStore;
import com.homa.listing.usecase.port.out.ProviderStandingRepository;

/**
 * A member the provider deactivated (HLP-5) or removed (HLP-8) stops counting towards the default maximum of helpers
 * (OFR-5). A supervisor, or a member Listing never counted, changes nothing. When the count changes and the provider
 * already has a listing, the terms snapshot's maximum of helpers has changed with it, so the full terms are
 * published again, as when a helper is activated. Whether the provider stays bookable with fewer helpers is
 * Provider's decision (PRV-2): Listing's standing waits for {@code ProviderBookabilityChangedIE}.
 */
public final class HandleTeamMemberDeactivatedIE implements TeamMemberDeactivatedIEHandler {

    /** Stable: never derived from the class name. */
    static final String HANDLER = "listing.TeamMemberDeactivated";

    private final ProcessedMessageStore processedMessages;
    private final ProviderStandingRepository standings;
    private final ListingRepository listings;
    private final ListingIEPublisher publisher;
    private final ListingEvents events;

    public HandleTeamMemberDeactivatedIE(ProcessedMessageStore processedMessages,
                                         ProviderStandingRepository standings, ListingRepository listings,
                                         ListingIEPublisher publisher, ListingEvents events) {
        this.processedMessages = processedMessages;
        this.standings = standings;
        this.listings = listings;
        this.publisher = publisher;
        this.events = events;
    }

    @Override
    public void handle(TeamMemberDeactivatedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        ProviderId providerId = new ProviderId(event.providerId());
        ProviderStanding standing = standings.findById(providerId).orElseGet(() -> ProviderStanding.unknown(providerId));
        boolean helpersChanged = standing.memberDeactivated(event.memberId());
        standings.save(standing);
        if (helpersChanged) {
            listings.findById(providerId).ifPresent(listing ->
                    publisher.publish(events.termsChanged(new ListingTermsChange(listing.terms(), false))));
        }
    }
}
