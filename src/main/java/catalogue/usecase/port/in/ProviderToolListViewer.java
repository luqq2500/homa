package catalogue.usecase.port.in;

import catalogue.usecase.dto.ViewToolListRequest;
import catalogue.usecase.dto.ViewToolListResponse;

/** A provider views the tools they can say they bring. */
public interface ProviderToolListViewer {

    ViewToolListResponse view(ViewToolListRequest request);
}
