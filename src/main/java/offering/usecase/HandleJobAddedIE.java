package offering.usecase;

import ie.events.JobAddedIE;
import offering.domain.OfferableCatalogue;
import offering.usecase.port.in.JobAddedIEHandler;
import offering.usecase.port.out.OfferableCatalogueRepository;

public final class HandleJobAddedIE implements JobAddedIEHandler {

    private final OfferableCatalogueRepository catalogues;

    public HandleJobAddedIE(OfferableCatalogueRepository catalogues) {
        this.catalogues = catalogues;
    }

    @Override
    public void handle(JobAddedIE event) {
        OfferableCatalogue catalogue = catalogues.get();
        catalogue.addJob(event.jobId());
        catalogues.save(catalogue);
    }
}
