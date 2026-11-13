package verification.usecase;

import ie.events.BusinessRegisteredIE;
import verification.domain.Business;
import verification.usecase.port.in.BusinessRegisteredIEHandler;
import verification.usecase.port.out.BusinessRepository;

public final class HandleBusinessRegisteredIE implements BusinessRegisteredIEHandler {

    private final BusinessRepository businesses;

    public HandleBusinessRegisteredIE(BusinessRepository businesses) {
        this.businesses = businesses;
    }

    @Override
    public void handle(BusinessRegisteredIE event) {
        if (businesses.find(event.businessId()).isEmpty()) {
            businesses.save(Business.register(event.businessId()));
        }
    }
}
