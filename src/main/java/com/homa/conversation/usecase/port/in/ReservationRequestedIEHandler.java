package com.homa.conversation.usecase.port.in;

import com.homa.ie.events.ReservationRequestedIE;

/** A customer's reservation is Requested: it opens the reservation's chat (CHT-1, CHT-2). */
public interface ReservationRequestedIEHandler {

    void handle(ReservationRequestedIE event);
}
