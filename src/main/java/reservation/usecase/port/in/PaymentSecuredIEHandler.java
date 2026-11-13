package reservation.usecase.port.in;

import ie.events.PaymentSecuredIE;

/** The provider now sees the request; an instant-book request is accepted at once. */
public interface PaymentSecuredIEHandler {

    void handle(PaymentSecuredIE event);
}
