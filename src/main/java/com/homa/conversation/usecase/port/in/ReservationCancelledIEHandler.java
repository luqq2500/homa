package com.homa.conversation.usecase.port.in;

import com.homa.ie.events.ReservationCancelledIE;

/** The reservation was cancelled: its chat is read-only at once only when that was before its Check-in Period (CHT-5). */
public interface ReservationCancelledIEHandler {

    void handle(ReservationCancelledIE event);
}
