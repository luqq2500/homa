package reservation.usecase.port.in;

import ie.events.PaymentFailedIE;

/** The request ends straight away and nothing is charged. */
public interface PaymentFailedIEHandler {

    void handle(PaymentFailedIE event);
}
