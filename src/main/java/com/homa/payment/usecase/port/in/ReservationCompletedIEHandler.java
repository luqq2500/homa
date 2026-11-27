package com.homa.payment.usecase.port.in;

import com.homa.ie.events.ReservationCompletedIE;

/** The provider marked the job Completed (JOB-3): its payout is owed once due (PAY-2). */
public interface ReservationCompletedIEHandler {

    void handle(ReservationCompletedIE event);
}
