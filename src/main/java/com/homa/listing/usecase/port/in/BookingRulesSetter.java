package com.homa.listing.usecase.port.in;

import com.homa.listing.usecase.dto.ProviderSetBookingRulesRequest;
import com.homa.listing.usecase.dto.ProviderSetBookingRulesResponse;

/** A provider sets its bounds, notice, buffer, policy and tier (OFR-5, OFR-6, OFR-9, OFR-10). */
public interface BookingRulesSetter {

    ProviderSetBookingRulesResponse set(ProviderSetBookingRulesRequest request);
}
