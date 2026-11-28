package com.homa.payment.usecase;

import com.homa.payment.usecase.port.out.PaymentGateway;
import java.util.ArrayList;
import java.util.List;

/**
 * Test only: every session started, every refund sent and every compensation paid, in order. Sessions are named for
 * the reference.
 */
public class RecordingPaymentGateway implements PaymentGateway {

    private final List<SessionRequest> sessions = new ArrayList<>();
    private final List<RefundRequest> refunds = new ArrayList<>();
    private final List<CompensationRequest> compensations = new ArrayList<>();

    @Override
    public Session startPayment(SessionRequest request) {
        sessions.add(request);
        return new Session("session-" + request.paymentReference(),
                "https://gateway.test/pay/" + request.paymentReference());
    }

    @Override
    public void refund(RefundRequest request) {
        refunds.add(request);
    }

    @Override
    public void payCompensation(CompensationRequest request) {
        compensations.add(request);
    }

    public List<SessionRequest> sessions() {
        return List.copyOf(sessions);
    }

    public List<RefundRequest> refunds() {
        return List.copyOf(refunds);
    }

    public List<CompensationRequest> compensations() {
        return List.copyOf(compensations);
    }
}
