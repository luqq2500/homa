package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.CompleteReservationRequest;
import com.homa.reservation.usecase.dto.CompleteReservationResponse;

/** JOB-3: the provider marks the job Completed with after-photos. */
public interface ReservationCompleter {

    CompleteReservationResponse complete(CompleteReservationRequest request);
}
