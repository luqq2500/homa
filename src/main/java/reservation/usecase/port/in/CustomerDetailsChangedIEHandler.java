package reservation.usecase.port.in;

import ie.events.CustomerDetailsChangedIE;

/** The new details are used for new requests; those already made keep theirs. */
public interface CustomerDetailsChangedIEHandler {

    void handle(CustomerDetailsChangedIE event);
}
