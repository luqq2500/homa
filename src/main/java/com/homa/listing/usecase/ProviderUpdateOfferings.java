package com.homa.listing.usecase;

import com.homa.listing.domain.Preset;
import com.homa.listing.usecase.dto.ProviderUpdateOfferingsRequest;
import com.homa.listing.usecase.dto.ProviderUpdateOfferingsResponse;
import com.homa.listing.usecase.port.in.OfferingUpdater;
import com.homa.listing.usecase.port.out.ListingIEPublisher;
import com.homa.listing.usecase.port.out.ListingOwners;
import com.homa.listing.usecase.port.out.ListingRepository;
import com.homa.listing.usecase.port.out.PresetRepository;

/** OFR-1: the provider's jobs, tools and cities, only from Homa's presets; publishes the full terms. */
public final class ProviderUpdateOfferings implements OfferingUpdater {

    private final PresetRepository presets;
    private final ListingChanges changes;

    public ProviderUpdateOfferings(ListingRepository listings, PresetRepository presets, ListingOwners owners,
                                   ListingIEPublisher publisher, ListingEvents events) {
        this.presets = presets;
        this.changes = new ListingChanges(listings, owners, publisher, events);
    }

    @Override
    public ProviderUpdateOfferingsResponse update(ProviderUpdateOfferingsRequest request) {
        changes.apply(request.actingPersonId(), request.providerId(), listing -> listing.chooseOfferings(
                OfferingCodes.offerings(request.jobCodes(), request.toolCodes(), request.cityCodes()),
                presets.find().orElseGet(Preset::empty)));
        return new ProviderUpdateOfferingsResponse(request.providerId());
    }
}
