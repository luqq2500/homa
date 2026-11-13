package reservation.usecase.port.in;

import ie.events.FlaggedReservationDecidedIE;

/** An admin cancelled or restored a reservation disputed for a serious flag. */
public interface FlaggedReservationDecidedIEHandler {

    void handle(FlaggedReservationDecidedIE event);
}
