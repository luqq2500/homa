package offering.usecase;

import java.time.Clock;
import offering.domain.Listing;
import offering.domain.Offering;
import offering.usecase.dto.SetHoursRequest;
import offering.usecase.dto.SetHoursResponse;
import offering.usecase.exception.HomaHoursNotKnownException;
import offering.usecase.port.in.ProviderHoursSetter;
import offering.usecase.port.out.HomaHoursRepository;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class ProviderSetHours implements ProviderHoursSetter {

    private final OfferingRepository offerings;
    private final HomaHoursRepository homaHours;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public ProviderSetHours(OfferingRepository offerings, HomaHoursRepository homaHours, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.homaHours = homaHours;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public SetHoursResponse set(SetHoursRequest request) {
        Offering offering = Offerings.offering(offerings, request.providerId());
        Listing before = offering.listing();
        offering.setHours(request.minHours(), request.maxHours(),
                homaHours.current().orElseThrow(HomaHoursNotKnownException::new));
        return new SetHoursResponse(Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant()));
    }
}
