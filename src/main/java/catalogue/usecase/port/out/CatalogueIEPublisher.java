package catalogue.usecase.port.out;

import ie.port.CatalogueIE;

/** Publishes the catalogue's integration events. The single publisher for this context. */
public interface CatalogueIEPublisher {

    void publish(CatalogueIE event);
}
