package catalogue.usecase.port.in;

import catalogue.usecase.dto.ViewShelvesRequest;
import catalogue.usecase.dto.ViewShelvesResponse;

/** A customer views the homepage shelves. */
public interface CustomerShelvesViewer {

    ViewShelvesResponse view(ViewShelvesRequest request);
}
