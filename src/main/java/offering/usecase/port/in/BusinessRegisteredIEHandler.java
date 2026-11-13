package offering.usecase.port.in;

import ie.events.BusinessRegisteredIE;

/** A registered business gets an empty offering to set up. */
public interface BusinessRegisteredIEHandler {

    void handle(BusinessRegisteredIE event);
}
