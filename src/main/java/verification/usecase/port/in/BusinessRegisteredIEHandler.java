package verification.usecase.port.in;

import ie.events.BusinessRegisteredIE;

/** A registered business starts unverified. */
public interface BusinessRegisteredIEHandler {

    void handle(BusinessRegisteredIE event);
}
