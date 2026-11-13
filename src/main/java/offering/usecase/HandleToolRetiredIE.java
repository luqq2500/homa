package offering.usecase;

import ie.events.ToolRetiredIE;
import ie.events.OfferingAdjustedIE;
import java.time.Clock;
import java.time.Instant;
import offering.domain.Listing;
import offering.domain.OfferableCatalogue;
import offering.domain.Offering;
import offering.usecase.port.in.ToolRetiredIEHandler;
import offering.usecase.port.out.OfferableCatalogueRepository;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class HandleToolRetiredIE implements ToolRetiredIEHandler {

    static final String ADJUSTMENT = "TOOL_RETIRED";

    private final OfferableCatalogueRepository catalogues;
    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public HandleToolRetiredIE(OfferableCatalogueRepository catalogues, OfferingRepository offerings,
                             OfferingIEPublisher publisher, Clock clock) {
        this.catalogues = catalogues;
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public void handle(ToolRetiredIE event) {
        Instant now = clock.instant();
        OfferableCatalogue catalogue = catalogues.get();
        catalogue.retireTool(event.toolId());
        catalogues.save(catalogue);
        for (Offering offering : offerings.bringingTool(event.toolId())) {
            Listing before = offering.listing();
            offering.retireTool(event.toolId());
            Offerings.saveAndAnnounce(offering, before, offerings, publisher, now);
            publisher.publish(new OfferingAdjustedIE(offering.providerId(), ADJUSTMENT, now));
        }
    }
}
