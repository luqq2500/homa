package com.homa.provider.usecase.port.in;

import com.homa.ie.events.ReservationCompletedIE;

/** Drops a Completed reservation's window (HLP-9). */
public interface ReservationCompletedIEHandler {

    void handle(ReservationCompletedIE event);
}
