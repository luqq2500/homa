package com.homa.payment.usecase;

import com.homa.ie.port.PaymentIE;
import com.homa.payment.usecase.port.out.PaymentIEPublisher;
import java.util.ArrayList;
import java.util.List;

/** Test only: every event published, in order. */
public class RecordingPaymentIEPublisher implements PaymentIEPublisher {

    private final List<PaymentIE> published = new ArrayList<>();

    @Override
    public void publish(PaymentIE event) {
        published.add(event);
    }

    public List<PaymentIE> published() {
        return List.copyOf(published);
    }

    public void clear() {
        published.clear();
    }
}
