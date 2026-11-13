package offering.usecase;

import ie.events.ToolAddedIE;
import offering.domain.OfferableCatalogue;
import offering.usecase.port.in.ToolAddedIEHandler;
import offering.usecase.port.out.OfferableCatalogueRepository;

public final class HandleToolAddedIE implements ToolAddedIEHandler {

    private final OfferableCatalogueRepository catalogues;

    public HandleToolAddedIE(OfferableCatalogueRepository catalogues) {
        this.catalogues = catalogues;
    }

    @Override
    public void handle(ToolAddedIE event) {
        OfferableCatalogue catalogue = catalogues.get();
        catalogue.addTool(event.toolId());
        catalogues.save(catalogue);
    }
}
