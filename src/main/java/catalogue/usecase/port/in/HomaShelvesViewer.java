package catalogue.usecase.port.in;

import catalogue.usecase.dto.HomaViewShelvesResponse;

/** Homa's founders view every homepage shelf, including those no longer shown. */
public interface HomaShelvesViewer {

    HomaViewShelvesResponse view();
}
