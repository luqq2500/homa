package com.homa.conversation.usecase.port.in;

import com.homa.ie.events.ReservationWithdrawnIE;

/** The customer withdrew the reservation: its chat is read-only at once (CHT-5). */
public interface ReservationWithdrawnIEHandler {

    void handle(ReservationWithdrawnIE event);
}
