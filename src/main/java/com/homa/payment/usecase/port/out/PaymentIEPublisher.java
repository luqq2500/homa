package com.homa.payment.usecase.port.out;

import com.homa.ie.port.PaymentIE;

/** Payment's single integration event publisher, called inside the use case's transaction (ADR 0002). */
public interface PaymentIEPublisher {

    void publish(PaymentIE event);
}
