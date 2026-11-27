package com.homa.listing.usecase.port.in;

import com.homa.listing.usecase.dto.ProviderSetPricingTermsRequest;
import com.homa.listing.usecase.dto.ProviderSetPricingTermsResponse;

/** A provider sets its rate, travel fee and discount (OFR-2 to OFR-4, OFR-11). */
public interface PricingTermsSetter {

    ProviderSetPricingTermsResponse set(ProviderSetPricingTermsRequest request);
}
