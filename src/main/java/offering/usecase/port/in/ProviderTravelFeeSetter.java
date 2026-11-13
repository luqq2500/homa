package offering.usecase.port.in;

import offering.usecase.dto.SetTravelFeeRequest;
import offering.usecase.dto.SetTravelFeeResponse;

/** A provider sets how the travel fee works: a separate flat fee, or included in the price. */
public interface ProviderTravelFeeSetter {

    SetTravelFeeResponse set(SetTravelFeeRequest request);
}
