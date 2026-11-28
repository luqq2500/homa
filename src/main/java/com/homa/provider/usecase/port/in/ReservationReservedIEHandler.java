package com.homa.provider.usecase.port.in;

import com.homa.ie.events.ReservationReservedIE;

/** Keeps a Reserved reservation's window, from the Check-in Period to the job end, for HLP-9. */
public interface ReservationReservedIEHandler {

    void handle(ReservationReservedIE event);
}
