package com.homa.payment.usecase.port.in;

import com.homa.payment.usecase.dto.PayForReservationRequest;
import com.homa.payment.usecase.dto.PayForReservationResponse;

/** A customer pays for a checkout through the gateway (PAY-1, RES-11, RES-12). */
public interface ReservationPayer {

    PayForReservationResponse pay(PayForReservationRequest request);
}
