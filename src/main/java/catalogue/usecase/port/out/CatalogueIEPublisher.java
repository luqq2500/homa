package catalogue.usecase.port.out;

import ie.port.CatalogueIE;

/** Publishes the catalogue's integration events to the other contexts. */
public interface CatalogueIEPublisher {

    void publish(CatalogueIE event);
}
