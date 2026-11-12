package catalogue.usecase.port.in;

import catalogue.usecase.dto.ProviderViewToolListResponse;

/** A provider views the tools they can say they bring. */
public interface ProviderToolListViewer {

    ProviderViewToolListResponse view();
}
