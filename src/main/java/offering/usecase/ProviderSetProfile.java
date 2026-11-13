package offering.usecase;

import java.time.Clock;
import offering.domain.Listing;
import offering.domain.Offering;
import offering.usecase.dto.SetProfileRequest;
import offering.usecase.dto.SetProfileResponse;
import offering.usecase.port.in.ProviderProfileSetter;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class ProviderSetProfile implements ProviderProfileSetter {

    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public ProviderSetProfile(OfferingRepository offerings, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public SetProfileResponse set(SetProfileRequest request) {
        Offering offering = Offerings.offering(offerings, request.providerId());
        Listing before = offering.listing();
        offering.setProfile(request.name(), request.picture());
        return new SetProfileResponse(Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant()));
    }
}
