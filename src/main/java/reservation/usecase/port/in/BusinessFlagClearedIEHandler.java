package reservation.usecase.port.in;

import ie.events.BusinessFlagClearedIE;

/** The business is no longer flagged. */
public interface BusinessFlagClearedIEHandler {

    void handle(BusinessFlagClearedIE event);
}
