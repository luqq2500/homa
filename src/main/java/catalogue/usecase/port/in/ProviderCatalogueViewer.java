package catalogue.usecase.port.in;

import catalogue.usecase.dto.ViewCatalogueResponse;

/** A provider sees the jobs, by group, and the tools they can pick from. */
public interface ProviderCatalogueViewer {

    ViewCatalogueResponse view();
}
