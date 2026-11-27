package com.homa.listing.usecase;

import com.homa.listing.domain.ListingTermsChange;
import com.homa.listing.domain.ProviderId;
import com.homa.listing.domain.ProviderListing;
import com.homa.listing.usecase.exception.ListingNotFoundException;
import com.homa.listing.usecase.port.out.ListingIEPublisher;
import com.homa.listing.usecase.port.out.ListingOwners;
import com.homa.listing.usecase.port.out.ListingRepository;
import java.util.function.Function;

/**
 * The steps every change to a listing's terms shares: only the owner may change it, the listing must exist, and
 * the saved terms are published in full as {@code ListingTermsChangedIE}.
 */
final class ListingChanges {

    private final ListingRepository listings;
    private final ListingOwners owners;
    private final ListingIEPublisher publisher;
    private final ListingEvents events;

    ListingChanges(ListingRepository listings, ListingOwners owners, ListingIEPublisher publisher, ListingEvents events) {
        this.listings = listings;
        this.owners = owners;
        this.publisher = publisher;
        this.events = events;
    }

    ListingTermsChange apply(String actingPersonId, String providerId, Function<ProviderListing, ListingTermsChange> change) {
        ProviderId id = new ProviderId(providerId);
        Ownership.requireOwner(owners, id, actingPersonId);
        ProviderListing listing = listings.findById(id).orElseThrow(() -> new ListingNotFoundException(providerId));
        ListingTermsChange changed = change.apply(listing);
        listings.save(listing);
        publisher.publish(events.termsChanged(changed));
        return changed;
    }
}
