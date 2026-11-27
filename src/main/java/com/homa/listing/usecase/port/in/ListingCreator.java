package com.homa.listing.usecase.port.in;

import com.homa.listing.usecase.dto.ProviderCreateListingRequest;
import com.homa.listing.usecase.dto.ProviderCreateListingResponse;

/** A provider's owner sets up its listing before going live (PRV-1, OFR-1, OFR-2, OFR-9, OFR-11). */
public interface ListingCreator {

    ProviderCreateListingResponse create(ProviderCreateListingRequest request);
}
