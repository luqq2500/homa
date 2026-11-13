package offering.usecase;

import java.time.Clock;
import offering.domain.Listing;
import offering.domain.Offering;
import offering.usecase.dto.SetHourlyRateRequest;
import offering.usecase.dto.SetHourlyRateResponse;
import offering.usecase.port.in.ProviderHourlyRateSetter;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class ProviderSetHourlyRate implements ProviderHourlyRateSetter {

    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public ProviderSetHourlyRate(OfferingRepository offerings, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public SetHourlyRateResponse set(SetHourlyRateRequest request) {
        Offering offering = Offerings.offering(offerings, request.providerId());
        Listing before = offering.listing();
        offering.setHourlyRate(request.hourlyRate());
        return new SetHourlyRateResponse(Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant()));
    }
}
