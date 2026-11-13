package offering.usecase.port.out;

import offering.domain.OfferableCatalogue;

/** The jobs and tools providers can pick, as the catalogue context announced them. */
public interface OfferableCatalogueRepository {

    OfferableCatalogue get();

    void save(OfferableCatalogue catalogue);
}
