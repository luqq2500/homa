package reservation.usecase.port.in;

import ie.events.OfferingChangedIE;

/** The terms new requests are made with. */
public interface OfferingChangedIEHandler {

    void handle(OfferingChangedIE event);
}
