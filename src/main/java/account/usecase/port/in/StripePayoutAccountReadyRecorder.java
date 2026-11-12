package account.usecase.port.in;

import account.usecase.dto.RecordPayoutAccountReadyRequest;

/** Stripe reports a business's payout account as ready. */
public interface StripePayoutAccountReadyRecorder {

    void record(RecordPayoutAccountReadyRequest request);
}
