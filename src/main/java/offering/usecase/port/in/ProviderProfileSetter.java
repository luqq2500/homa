package offering.usecase.port.in;

import offering.usecase.dto.SetProfileRequest;
import offering.usecase.dto.SetProfileResponse;

/** A provider sets their profile name and picture. */
public interface ProviderProfileSetter {

    SetProfileResponse set(SetProfileRequest request);
}
