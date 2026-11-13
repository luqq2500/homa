package offering.usecase.port.in;

import offering.usecase.dto.SetHoursRequest;
import offering.usecase.dto.SetHoursResponse;

/** A provider sets their minimum and maximum hours, within Homa's range. */
public interface ProviderHoursSetter {

    SetHoursResponse set(SetHoursRequest request);
}
