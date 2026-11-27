package com.homa.verification.usecase.port.in;

import com.homa.ie.events.ProviderOpenedIE;

/** Records a newly opened provider's owner, whose identity its payout account is checked against (PRV-1, PRV-2). */
public interface ProviderOpenedIEHandler {

    void handle(ProviderOpenedIE event);
}
