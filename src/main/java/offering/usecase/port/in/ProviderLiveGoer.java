package offering.usecase.port.in;

import offering.usecase.dto.GoLiveRequest;
import offering.usecase.dto.GoLiveResponse;

/** A provider goes live once everything is set up; otherwise Homa lists everything still missing. */
public interface ProviderLiveGoer {

    GoLiveResponse goLive(GoLiveRequest request);
}
