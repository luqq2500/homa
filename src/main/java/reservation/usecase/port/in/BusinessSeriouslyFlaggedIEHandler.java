package reservation.usecase.port.in;

import ie.events.BusinessSeriouslyFlaggedIE;

/** Every accepted reservation whose completion has not been submitted moves to Disputed for the flag. */
public interface BusinessSeriouslyFlaggedIEHandler {

    void handle(BusinessSeriouslyFlaggedIE event);
}
