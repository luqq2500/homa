package offering.usecase;

import ie.events.PayoutAccountAddedIE;
import java.time.Clock;
import offering.domain.Listing;
import offering.usecase.port.in.PayoutAccountAddedIEHandler;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class HandlePayoutAccountAddedIE implements PayoutAccountAddedIEHandler {

    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public HandlePayoutAccountAddedIE(OfferingRepository offerings, OfferingIEPublisher publisher, Clock clock) {
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public void handle(PayoutAccountAddedIE event) {
        offerings.find(event.providerId()).ifPresent(offering -> {
            Listing before = offering.listing();
            offering.payoutAccountAdded();
            Offerings.saveAndAnnounce(offering, before, offerings, publisher, clock.instant());
        });
    }
}
