package offering.usecase.port.in;

import ie.events.BusinessVerifiedIE;

/** A verified business can go live; a paused listing resumes. */
public interface BusinessVerifiedIEHandler {

    void handle(BusinessVerifiedIE event);
}
