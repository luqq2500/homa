package catalogue.usecase.port.in;

import catalogue.usecase.dto.ViewServiceAreaRequest;
import catalogue.usecase.dto.ViewServiceAreaResponse;

/** A provider views the cities they can cover. */
public interface ProviderServiceAreaViewer {

    ViewServiceAreaResponse view(ViewServiceAreaRequest request);
}
