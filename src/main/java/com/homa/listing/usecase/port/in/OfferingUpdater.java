package com.homa.listing.usecase.port.in;

import com.homa.listing.usecase.dto.ProviderUpdateOfferingsRequest;
import com.homa.listing.usecase.dto.ProviderUpdateOfferingsResponse;

/** A provider changes its jobs, tools and cities (OFR-1). */
public interface OfferingUpdater {

    ProviderUpdateOfferingsResponse update(ProviderUpdateOfferingsRequest request);
}
