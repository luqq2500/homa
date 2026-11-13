package offering.usecase;

import java.time.Clock;
import offering.domain.Listing;
import offering.domain.Offering;
import offering.usecase.dto.SetJobsRequest;
import offering.usecase.dto.SetJobsResponse;
import offering.usecase.port.in.ProviderJobsSetter;
import offering.usecase.port.out.OfferableCatalogueRepository;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class ProviderSetJobs implements ProviderJobsSetter {

    private final OfferingRepository offerings;
    private final OfferableCatalogueRepository catalogues;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public ProviderSetJobs(OfferingRepository offerings, OfferableCatalogueRepository catalogues, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.catalogues = catalogues;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public SetJobsResponse set(SetJobsRequest request) {
        Offering offering = Offerings.offering(offerings, request.providerId());
        Listing before = offering.listing();
        offering.offerJobs(request.jobIds(), catalogues.get());
        return new SetJobsResponse(Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant()));
    }
}
