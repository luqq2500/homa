package com.homa.verification.usecase.port.in;

import com.homa.ie.events.PayoutAccountSubmittedIE;

/** Checks a submitted payout account's holder through the payment provider (PRV-2, PRV-10, IDN-3). */
public interface PayoutAccountSubmittedIEHandler {

    void handle(PayoutAccountSubmittedIE event);
}
