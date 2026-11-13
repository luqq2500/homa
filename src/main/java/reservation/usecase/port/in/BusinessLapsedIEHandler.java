package reservation.usecase.port.in;

import ie.events.BusinessLapsedIE;

/** A lapsed business takes no new requests. */
public interface BusinessLapsedIEHandler {

    void handle(BusinessLapsedIE event);
}
