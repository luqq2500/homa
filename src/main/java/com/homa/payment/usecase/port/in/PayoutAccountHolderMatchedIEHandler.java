package com.homa.payment.usecase.port.in;

import com.homa.ie.events.PayoutAccountHolderMatchedIE;

/** Verification checked a payout account's holder (PRV-2). */
public interface PayoutAccountHolderMatchedIEHandler {

    void handle(PayoutAccountHolderMatchedIE event);
}
