package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.ProviderCancelReservationRequest;
import com.homa.reservation.usecase.dto.ProviderCancelReservationResponse;

/** PCX-1, PCX-2, LMC-3: the provider cancels a Reserved reservation. */
public interface ProviderReservationCanceller {

    ProviderCancelReservationResponse cancel(ProviderCancelReservationRequest request);
}
