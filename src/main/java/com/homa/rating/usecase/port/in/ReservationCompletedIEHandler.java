package com.homa.rating.usecase.port.in;

import com.homa.ie.events.ReservationCompletedIE;

/** The provider marked the job Completed (JOB-3): the Rating Window opens (RTG-2). */
public interface ReservationCompletedIEHandler {

    void handle(ReservationCompletedIE event);
}
