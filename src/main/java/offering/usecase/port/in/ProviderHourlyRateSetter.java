package offering.usecase.port.in;

import offering.usecase.dto.SetHourlyRateRequest;
import offering.usecase.dto.SetHourlyRateResponse;

/** A provider sets their flat hourly rate per helper. */
public interface ProviderHourlyRateSetter {

    SetHourlyRateResponse set(SetHourlyRateRequest request);
}
