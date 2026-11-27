package com.homa.listing.usecase.ie;

import com.homa.ie.events.TeamMemberActivatedIE;
import com.homa.listing.domain.ListingTermsChange;
import com.homa.listing.domain.MemberRole;
import com.homa.listing.domain.ProviderId;
import com.homa.listing.domain.ProviderStanding;
import com.homa.listing.usecase.ListingEvents;
import com.homa.listing.usecase.port.in.TeamMemberActivatedIEHandler;
import com.homa.listing.usecase.port.out.ListingIEPublisher;
import com.homa.listing.usecase.port.out.ListingRepository;
import com.homa.listing.usecase.port.out.ProcessedMessageStore;
import com.homa.listing.usecase.port.out.ProviderStandingRepository;

/**
 * An active helper counts towards the default maximum of helpers (OFR-5); a supervisor doesn't (ROS-2). When the
 * count changes and the provider already has a listing, the terms snapshot's maximum of helpers may have changed
 * with it, so the full terms are published again.
 */
public final class HandleTeamMemberActivatedIE implements TeamMemberActivatedIEHandler {

    /** Stable: never derived from the class name. */
    static final String HANDLER = "listing.TeamMemberActivated";

    private final ProcessedMessageStore processedMessages;
    private final ProviderStandingRepository standings;
    private final ListingRepository listings;
    private final ListingIEPublisher publisher;
    private final ListingEvents events;

    public HandleTeamMemberActivatedIE(ProcessedMessageStore processedMessages, ProviderStandingRepository standings,
                                       ListingRepository listings, ListingIEPublisher publisher, ListingEvents events) {
        this.processedMessages = processedMessages;
        this.standings = standings;
        this.listings = listings;
        this.publisher = publisher;
        this.events = events;
    }

    @Override
    public void handle(TeamMemberActivatedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        ProviderId providerId = new ProviderId(event.providerId());
        ProviderStanding standing = standings.findById(providerId).orElseGet(() -> ProviderStanding.unknown(providerId));
        boolean helpersChanged = standing.memberActivated(event.memberId(), MemberRole.valueOf(event.role()));
        standings.save(standing);
        if (helpersChanged) {
            listings.findById(providerId).ifPresent(listing ->
                    publisher.publish(events.termsChanged(new ListingTermsChange(listing.terms(), false))));
        }
    }
}
