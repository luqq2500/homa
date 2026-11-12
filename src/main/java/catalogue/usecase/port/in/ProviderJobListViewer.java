package catalogue.usecase.port.in;

import catalogue.usecase.dto.ProviderViewJobListResponse;

/** A provider views the jobs they can offer, by group. */
public interface ProviderJobListViewer {

    ProviderViewJobListResponse view();
}
