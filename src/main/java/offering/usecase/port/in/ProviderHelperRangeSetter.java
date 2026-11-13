package offering.usecase.port.in;

import offering.usecase.dto.SetHelperRangeRequest;
import offering.usecase.dto.SetHelperRangeResponse;

/** A provider sets the minimum and maximum number of helpers per booking. */
public interface ProviderHelperRangeSetter {

    SetHelperRangeResponse set(SetHelperRangeRequest request);
}
