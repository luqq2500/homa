package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.AcceptReservationRequest;
import com.homa.reservation.usecase.dto.AcceptReservationResponse;

/** ACC-2: the provider accepts a Requested reservation. */
public interface ReservationAccepter {

    AcceptReservationResponse accept(AcceptReservationRequest request);
}
