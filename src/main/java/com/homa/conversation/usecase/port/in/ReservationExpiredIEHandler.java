package com.homa.conversation.usecase.port.in;

import com.homa.ie.events.ReservationExpiredIE;

/** The reservation expired unanswered: its chat is read-only at once (CHT-5). */
public interface ReservationExpiredIEHandler {

    void handle(ReservationExpiredIE event);
}
