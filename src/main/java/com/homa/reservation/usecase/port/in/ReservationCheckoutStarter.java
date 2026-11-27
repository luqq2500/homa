package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.StartReservationCheckoutRequest;
import com.homa.reservation.usecase.dto.StartReservationCheckoutResponse;

/** RES-1 to RES-5, RES-9 to RES-11, ACC-3, CUS-3: the customer starts paying; nothing is held. */
public interface ReservationCheckoutStarter {

    StartReservationCheckoutResponse start(StartReservationCheckoutRequest request);
}
