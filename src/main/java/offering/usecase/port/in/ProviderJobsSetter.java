package offering.usecase.port.in;

import offering.usecase.dto.SetJobsRequest;
import offering.usecase.dto.SetJobsResponse;

/** A provider chooses the jobs they offer from Homa's job list. */
public interface ProviderJobsSetter {

    SetJobsResponse set(SetJobsRequest request);
}
