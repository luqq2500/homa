package catalogue.usecase.port.in;

import catalogue.usecase.dto.HomaViewShowcaseResponse;

/** Homa's founders view every featured search, including those no longer shown. */
public interface HomaShowcaseViewer {

    HomaViewShowcaseResponse view();
}
