package offering.usecase;

import ie.events.BusinessRegisteredIE;
import offering.domain.Offering;
import offering.usecase.port.in.BusinessRegisteredIEHandler;
import offering.usecase.port.out.OfferingRepository;

public final class HandleBusinessRegisteredIE implements BusinessRegisteredIEHandler {

    private final OfferingRepository offerings;

    public HandleBusinessRegisteredIE(OfferingRepository offerings) {
        this.offerings = offerings;
    }

    @Override
    public void handle(BusinessRegisteredIE event) {
        if (offerings.find(event.businessId()).isEmpty()) {
            offerings.save(Offering.startFor(event.businessId()));
        }
    }
}
