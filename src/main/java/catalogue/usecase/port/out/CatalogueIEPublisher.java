package catalogue.usecase.port.out;

import ie.port.CatalogueIE;

public interface CatalogueIEPublisher {

    void publish(CatalogueIE event);
}
