package catalogue.usecase.port.in;

import catalogue.usecase.dto.CustomerViewShowcaseResponse;

/** A customer views the featured searches on the homepage, in order. */
public interface CustomerShowcaseViewer {

    CustomerViewShowcaseResponse view();
}
