package availability.usecase.port.in;

import availability.usecase.dto.SetAvailabilityRequest;
import availability.usecase.dto.SetAvailabilityResponse;

/** A provider sets the times they are available. */
public interface ProviderAvailabilitySetter {

    SetAvailabilityResponse set(SetAvailabilityRequest request);
}
