package offering.usecase;

import ie.events.BusinessFlagClearedIE;
import java.time.Clock;
import offering.domain.Listing;
import offering.usecase.port.in.BusinessFlagClearedIEHandler;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class HandleBusinessFlagClearedIE implements BusinessFlagClearedIEHandler {

    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public HandleBusinessFlagClearedIE(OfferingRepository offerings, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    /** Only a business that is verified again resumes; one that had lapsed before the flag stays paused. */
    @Override
    public void handle(BusinessFlagClearedIE event) {
        if (!event.verified()) {
            return;
        }
        offerings.find(event.businessId()).ifPresent(offering -> {
            Listing before = offering.listing();
            offering.businessVerified();
            Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant());
        });
    }
}
