package offering.usecase;

import java.time.Clock;
import offering.domain.Listing;
import offering.domain.Offering;
import offering.usecase.dto.SetHelperRangeRequest;
import offering.usecase.dto.SetHelperRangeResponse;
import offering.usecase.port.in.ProviderHelperRangeSetter;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class ProviderSetHelperRange implements ProviderHelperRangeSetter {

    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public ProviderSetHelperRange(OfferingRepository offerings, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public SetHelperRangeResponse set(SetHelperRangeRequest request) {
        Offering offering = Offerings.offering(offerings, request.providerId());
        Listing before = offering.listing();
        offering.setHelperRange(request.minHelpers(), request.maxHelpers());
        return new SetHelperRangeResponse(Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant()));
    }
}
