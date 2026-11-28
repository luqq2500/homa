package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.DeclineReservationRequest;
import com.homa.reservation.usecase.dto.DeclineReservationResponse;

/** REQ-3: the provider declines a Requested reservation. */
public interface ReservationDecliner {

    DeclineReservationResponse decline(DeclineReservationRequest request);
}
