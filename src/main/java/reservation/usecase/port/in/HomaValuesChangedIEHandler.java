package reservation.usecase.port.in;

import ie.events.HomaValuesChangedIE;

/** The values new reservations are requested with. */
public interface HomaValuesChangedIEHandler {

    void handle(HomaValuesChangedIE event);
}
