package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.CustomerCancelReservationRequest;
import com.homa.reservation.usecase.dto.CustomerCancelReservationResponse;

/** CCX-1 to CCX-6, LMC-1, LMC-2: the customer cancels a Reserved reservation. */
public interface CustomerReservationCanceller {

    CustomerCancelReservationResponse cancel(CustomerCancelReservationRequest request);
}
