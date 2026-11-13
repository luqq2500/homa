package reservation.usecase.port.in;

import ie.events.ProviderCalendarChangedIE;

/** The provider's open time and buffer. */
public interface ProviderCalendarChangedIEHandler {

    void handle(ProviderCalendarChangedIE event);
}
