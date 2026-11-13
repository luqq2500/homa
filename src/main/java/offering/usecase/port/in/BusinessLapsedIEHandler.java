package offering.usecase.port.in;

import ie.events.BusinessLapsedIE;

/** A lapsed business has its listing paused. */
public interface BusinessLapsedIEHandler {

    void handle(BusinessLapsedIE event);
}
