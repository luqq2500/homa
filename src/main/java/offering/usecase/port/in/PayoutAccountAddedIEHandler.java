package offering.usecase.port.in;

import ie.events.PayoutAccountAddedIE;

/** A payout account is one of the things needed to go live. */
public interface PayoutAccountAddedIEHandler {

    void handle(PayoutAccountAddedIE event);
}
