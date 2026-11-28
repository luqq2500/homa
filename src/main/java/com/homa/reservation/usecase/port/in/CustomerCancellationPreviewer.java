package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.PreviewCustomerCancellationRequest;
import com.homa.reservation.usecase.dto.PreviewCustomerCancellationResponse;

/** CCX-1 to CCX-6, LMC-1, LMC-2: the customer sees the fee before confirming a cancellation. */
public interface CustomerCancellationPreviewer {

    PreviewCustomerCancellationResponse preview(PreviewCustomerCancellationRequest request);
}
