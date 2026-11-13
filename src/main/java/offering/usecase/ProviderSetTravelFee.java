package offering.usecase;

import java.time.Clock;
import offering.domain.Listing;
import offering.domain.Offering;
import offering.domain.TravelFee;
import offering.usecase.dto.SetTravelFeeRequest;
import offering.usecase.dto.SetTravelFeeResponse;
import offering.usecase.port.in.ProviderTravelFeeSetter;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class ProviderSetTravelFee implements ProviderTravelFeeSetter {

    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public ProviderSetTravelFee(OfferingRepository offerings, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public SetTravelFeeResponse set(SetTravelFeeRequest request) {
        Offering offering = Offerings.offering(offerings, request.providerId());
        Listing before = offering.listing();
        offering.setTravelFee(new TravelFee(request.separate(), request.amount()));
        return new SetTravelFeeResponse(Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant()));
    }
}
