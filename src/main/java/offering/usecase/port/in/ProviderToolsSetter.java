package offering.usecase.port.in;

import offering.usecase.dto.SetToolsRequest;
import offering.usecase.dto.SetToolsResponse;

/** A provider chooses the tools they bring from Homa's tool list. */
public interface ProviderToolsSetter {

    SetToolsResponse set(SetToolsRequest request);
}
