package com.homa.provider.usecase.port.in;

import com.homa.ie.events.PayoutAccountVerifiedIE;

/** Records whether the provider has a verified payout account in its name, and re-checks bookability (PRV-2). */
public interface PayoutAccountVerifiedIEHandler {

    void handle(PayoutAccountVerifiedIE event);
}
