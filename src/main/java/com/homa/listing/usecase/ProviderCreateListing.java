package com.homa.listing.usecase;

import com.homa.listing.domain.AcceptancePolicy;
import com.homa.listing.domain.CancellationTier;
import com.homa.listing.domain.HourlyRate;
import com.homa.listing.domain.ListingTermsChange;
import com.homa.listing.domain.Preset;
import com.homa.listing.domain.ProviderId;
import com.homa.listing.domain.ProviderListing;
import com.homa.listing.usecase.dto.ProviderCreateListingRequest;
import com.homa.listing.usecase.dto.ProviderCreateListingResponse;
import com.homa.listing.usecase.exception.ListingAlreadyExistsException;
import com.homa.listing.usecase.port.in.ListingCreator;
import com.homa.listing.usecase.port.out.ListingIEPublisher;
import com.homa.listing.usecase.port.out.ListingOwners;
import com.homa.listing.usecase.port.out.ListingRepository;
import com.homa.listing.usecase.port.out.PresetRepository;

/**
 * A provider sets up its one listing, usually before it goes live. The plan names no use case for this; a listing
 * must exist before its terms can change, so this one creates it with the domain's defaults for everything the
 * provider hasn't chosen, records the acting person as its owner, and publishes the first terms snapshot.
 */
public final class ProviderCreateListing implements ListingCreator {

    private final ListingRepository listings;
    private final PresetRepository presets;
    private final ListingOwners owners;
    private final ListingIEPublisher publisher;
    private final ListingEvents events;

    public ProviderCreateListing(ListingRepository listings, PresetRepository presets, ListingOwners owners,
                                 ListingIEPublisher publisher, ListingEvents events) {
        this.listings = listings;
        this.presets = presets;
        this.owners = owners;
        this.publisher = publisher;
        this.events = events;
    }

    @Override
    public ProviderCreateListingResponse create(ProviderCreateListingRequest request) {
        ProviderId providerId = new ProviderId(request.providerId());
        Ownership.requireNoOtherOwner(owners, providerId, request.actingPersonId());
        if (listings.findById(providerId).isPresent()) {
            throw new ListingAlreadyExistsException(request.providerId());
        }
        ProviderListing listing = ProviderListing.create(
                providerId,
                OfferingCodes.offerings(request.jobCodes(), request.toolCodes(), request.cityCodes()),
                presets.find().orElseGet(Preset::empty),
                new HourlyRate(request.hourlyRatePerHelper()),
                AcceptancePolicy.valueOf(request.acceptancePolicy()),
                CancellationTier.valueOf(request.cancellationTier()));
        listings.save(listing);
        owners.record(providerId, request.actingPersonId());
        publisher.publish(events.termsChanged(new ListingTermsChange(listing.terms(), false)));
        return new ProviderCreateListingResponse(request.providerId());
    }
}
