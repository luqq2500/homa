package catalogue.usecase.port.in;

import catalogue.usecase.dto.ProviderViewServiceAreaResponse;

/** A provider views the cities they can cover. */
public interface ProviderServiceAreaViewer {

    ProviderViewServiceAreaResponse view();
}
