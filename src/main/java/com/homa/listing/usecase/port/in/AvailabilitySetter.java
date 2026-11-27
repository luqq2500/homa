package com.homa.listing.usecase.port.in;

import com.homa.listing.usecase.dto.ProviderSetAvailabilityRequest;
import com.homa.listing.usecase.dto.ProviderSetAvailabilityResponse;

/** A provider sets its weekly working hours (OFR-8); calendar sync comes later. */
public interface AvailabilitySetter {

    ProviderSetAvailabilityResponse set(ProviderSetAvailabilityRequest request);
}
