package catalogue.usecase.port.in;

import catalogue.usecase.dto.HomaViewJobListResponse;

/** Homa's founders view the whole job list, retired jobs included. */
public interface HomaJobListViewer {

    HomaViewJobListResponse view();
}
