package catalogue.usecase.port.in;

import catalogue.usecase.dto.CustomerViewShelvesResponse;

/** A customer views the homepage shelves, in order. */
public interface CustomerShelvesViewer {

    CustomerViewShelvesResponse view();
}
