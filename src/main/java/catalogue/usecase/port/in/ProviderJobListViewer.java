package catalogue.usecase.port.in;

import catalogue.usecase.dto.ViewJobListRequest;
import catalogue.usecase.dto.ViewJobListResponse;

/** A provider views the jobs they can offer. */
public interface ProviderJobListViewer {

    ViewJobListResponse view(ViewJobListRequest request);
}
