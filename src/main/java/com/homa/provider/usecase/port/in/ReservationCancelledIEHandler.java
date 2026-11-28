package com.homa.provider.usecase.port.in;

import com.homa.ie.events.ReservationCancelledIE;

/** Drops a Cancelled reservation's window (HLP-9). */
public interface ReservationCancelledIEHandler {

    void handle(ReservationCancelledIE event);
}
