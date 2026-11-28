package com.homa.reservation.usecase;

import com.homa.reservation.domain.Cancellation;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.usecase.dto.PreviewCustomerCancellationRequest;
import com.homa.reservation.usecase.dto.PreviewCustomerCancellationResponse;
import com.homa.reservation.usecase.port.in.CustomerCancellationPreviewer;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import com.homa.reservation.usecase.port.out.TimeSource;

/**
 * CCX-1 to CCX-6, LMC-1, LMC-2: the customer sees the fee cancelling would cost now, and what comes back, before
 * confirming. The reservation works it out; nothing is changed, saved or published.
 */
public final class PreviewCustomerCancellation implements CustomerCancellationPreviewer {

    private final ReservationRepository reservations;
    private final TimeSource time;

    public PreviewCustomerCancellation(ReservationRepository reservations, TimeSource time) {
        this.reservations = reservations;
        this.time = time;
    }

    @Override
    public PreviewCustomerCancellationResponse preview(PreviewCustomerCancellationRequest request) {
        Reservation reservation = ReservationParties.requireCustomer(
                ReservationParties.load(reservations, request.reservationId()), request.customerId());
        Cancellation previewed = reservation.previewCustomerCancellation(time.now());
        return new PreviewCustomerCancellationResponse(reservation.id().value(), previewed.cause().name(),
                previewed.at(), previewed.settlement().customerFee(), previewed.settlement().customerRefund());
    }
}
