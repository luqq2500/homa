package com.homa.reservation.usecase.port.out;

import com.homa.ie.port.ReservationIE;

/** The Reservation context's single integration event publisher, called inside the use case's transaction. */
public interface ReservationIEPublisher {

    void publish(ReservationIE event);
}
