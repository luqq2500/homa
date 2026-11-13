package offering.usecase.port.in;

import offering.usecase.dto.SetCitiesRequest;
import offering.usecase.dto.SetCitiesResponse;

/** A provider chooses the cities they cover. */
public interface ProviderCitiesSetter {

    SetCitiesResponse set(SetCitiesRequest request);
}
