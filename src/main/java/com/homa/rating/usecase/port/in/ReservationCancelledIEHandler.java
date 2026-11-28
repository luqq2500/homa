package com.homa.rating.usecase.port.in;

import com.homa.ie.events.ReservationCancelledIE;

/**
 * A Reserved reservation was cancelled: it counts in reliability by its cause (RTG-10), and a Provider No Show voids
 * the ratings of a job marked Completed (NSH-6).
 */
public interface ReservationCancelledIEHandler {

    void handle(ReservationCancelledIE event);
}
