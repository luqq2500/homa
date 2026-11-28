package com.homa.conversation.usecase.port.in;

import com.homa.ie.events.ReservationDeclinedIE;

/** The provider declined the reservation: its chat is read-only at once (CHT-5). */
public interface ReservationDeclinedIEHandler {

    void handle(ReservationDeclinedIE event);
}
