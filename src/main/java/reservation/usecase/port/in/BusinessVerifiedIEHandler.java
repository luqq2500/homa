package reservation.usecase.port.in;

import ie.events.BusinessVerifiedIE;

/** The date the business is verified until. */
public interface BusinessVerifiedIEHandler {

    void handle(BusinessVerifiedIE event);
}
