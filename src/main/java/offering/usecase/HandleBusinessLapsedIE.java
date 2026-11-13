package offering.usecase;

import ie.events.BusinessLapsedIE;
import java.time.Clock;
import offering.domain.Listing;
import offering.usecase.port.in.BusinessLapsedIEHandler;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class HandleBusinessLapsedIE implements BusinessLapsedIEHandler {

    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public HandleBusinessLapsedIE(OfferingRepository offerings, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public void handle(BusinessLapsedIE event) {
        offerings.find(event.businessId()).ifPresent(offering -> {
            Listing before = offering.listing();
            offering.businessLapsed();
            Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant());
        });
    }
}
