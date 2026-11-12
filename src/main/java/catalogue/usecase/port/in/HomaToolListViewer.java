package catalogue.usecase.port.in;

import catalogue.usecase.dto.HomaViewToolListResponse;

/** Homa's founders view the whole tool list, retired tools included. */
public interface HomaToolListViewer {

    HomaViewToolListResponse view();
}
