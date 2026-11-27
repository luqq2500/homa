package com.homa.payment.usecase.port.in;

import com.homa.payment.usecase.dto.ConfirmGatewayPaymentRequest;
import com.homa.payment.usecase.dto.ConfirmGatewayPaymentResponse;

/** The gateway's webhook: a payment attempt was confirmed or refused (EXT-2, PAY-1, RES-12). */
public interface GatewayPaymentConfirmer {

    ConfirmGatewayPaymentResponse confirm(ConfirmGatewayPaymentRequest request);
}
