package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.WithdrawReservationRequest;
import com.homa.reservation.usecase.dto.WithdrawReservationResponse;

/** REQ-2, REQ-5: the customer withdraws a Requested reservation. */
public interface ReservationWithdrawer {

    WithdrawReservationResponse withdraw(WithdrawReservationRequest request);
}
