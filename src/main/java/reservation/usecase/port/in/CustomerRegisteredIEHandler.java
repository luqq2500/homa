package reservation.usecase.port.in;

import ie.events.CustomerRegisteredIE;

/** A customer who can now request. */
public interface CustomerRegisteredIEHandler {

    void handle(CustomerRegisteredIE event);
}
