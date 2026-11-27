package com.homa.payment.usecase;

import com.homa.payment.usecase.port.out.PayoutGateway;
import java.util.ArrayList;
import java.util.List;

/**
 * Test only: every account registered and every transfer sent, in order. The n-th registration gets the reference
 * gateway-account-n.
 */
public class RecordingPayoutGateway implements PayoutGateway {

    private final List<AccountRegistration> registrations = new ArrayList<>();
    private final List<Transfer> transfers = new ArrayList<>();

    @Override
    public String registerAccount(AccountRegistration registration) {
        registrations.add(registration);
        return "gateway-account-" + registrations.size();
    }

    @Override
    public void send(Transfer transfer) {
        transfers.add(transfer);
    }

    public List<AccountRegistration> registrations() {
        return List.copyOf(registrations);
    }

    public List<Transfer> transfers() {
        return List.copyOf(transfers);
    }
}
