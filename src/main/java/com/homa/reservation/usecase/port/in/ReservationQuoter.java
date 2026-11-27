package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.QuoteReservationRequest;
import com.homa.reservation.usecase.dto.QuoteReservationResponse;

/** RES-6, OFR-4: the price of a booking. */
public interface ReservationQuoter {

    QuoteReservationResponse quote(QuoteReservationRequest request);
}
