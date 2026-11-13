package offering.usecase;

import ie.events.BusinessVerifiedIE;
import java.time.Clock;
import offering.domain.Listing;
import offering.usecase.port.in.BusinessVerifiedIEHandler;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class HandleBusinessVerifiedIE implements BusinessVerifiedIEHandler {

    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public HandleBusinessVerifiedIE(OfferingRepository offerings, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public void handle(BusinessVerifiedIE event) {
        offerings.find(event.businessId()).ifPresent(offering -> {
            Listing before = offering.listing();
            offering.businessVerified();
            Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant());
        });
    }
}
