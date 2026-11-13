package offering.usecase;

import java.time.Clock;
import offering.domain.Listing;
import offering.domain.Offering;
import offering.usecase.dto.SetToolsRequest;
import offering.usecase.dto.SetToolsResponse;
import offering.usecase.port.in.ProviderToolsSetter;
import offering.usecase.port.out.OfferableCatalogueRepository;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class ProviderSetTools implements ProviderToolsSetter {

    private final OfferingRepository offerings;
    private final OfferableCatalogueRepository catalogues;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public ProviderSetTools(OfferingRepository offerings, OfferableCatalogueRepository catalogues, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.catalogues = catalogues;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public SetToolsResponse set(SetToolsRequest request) {
        Offering offering = Offerings.offering(offerings, request.providerId());
        Listing before = offering.listing();
        offering.bringTools(request.toolIds(), catalogues.get());
        return new SetToolsResponse(Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant()));
    }
}
