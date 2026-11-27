package com.homa.payment.usecase.port.in;

import com.homa.payment.usecase.dto.SubmitPayoutAccountRequest;
import com.homa.payment.usecase.dto.SubmitPayoutAccountResponse;

/** A provider's owner submits the payout account its payouts go to (PRV-1, PRV-2). */
public interface PayoutAccountSubmitter {

    SubmitPayoutAccountResponse submit(SubmitPayoutAccountRequest request);
}
