package offering.usecase;

import ie.events.JobRetiredIE;
import ie.events.OfferingAdjustedIE;
import java.time.Clock;
import java.time.Instant;
import offering.domain.Listing;
import offering.domain.OfferableCatalogue;
import offering.domain.Offering;
import offering.usecase.port.in.JobRetiredIEHandler;
import offering.usecase.port.out.OfferableCatalogueRepository;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class HandleJobRetiredIE implements JobRetiredIEHandler {

    static final String ADJUSTMENT = "JOB_RETIRED";

    private final OfferableCatalogueRepository catalogues;
    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public HandleJobRetiredIE(OfferableCatalogueRepository catalogues, OfferingRepository offerings,
                             OfferingIEPublisher publisher, Clock clock) {
        this.catalogues = catalogues;
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public void handle(JobRetiredIE event) {
        Instant now = clock.instant();
        OfferableCatalogue catalogue = catalogues.get();
        catalogue.retireJob(event.jobId());
        catalogues.save(catalogue);
        for (Offering offering : offerings.offeringJob(event.jobId())) {
            Listing before = offering.listing();
            offering.retireJob(event.jobId());
            Offerings.saveAndAnnounce(offering, before, offerings, publisher, now);
            publisher.publish(new OfferingAdjustedIE(offering.providerId(), ADJUSTMENT, now));
        }
    }
}
