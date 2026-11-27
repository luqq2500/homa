package com.homa.payment.usecase;

import com.homa.payment.usecase.port.out.PayoutGateway;
import java.util.ArrayList;
import java.util.List;

/** Test only: every transfer sent, in order. */
public class RecordingPayoutGateway implements PayoutGateway {

    private final List<Transfer> transfers = new ArrayList<>();

    @Override
    public void send(Transfer transfer) {
        transfers.add(transfer);
    }

    public List<Transfer> transfers() {
        return List.copyOf(transfers);
    }
}
