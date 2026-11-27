package com.homa.payment.usecase.port.in;

import com.homa.ie.events.ProviderOpenedIE;

/** Records a newly opened provider's owner, the one person who may submit its payout account (PRV-1, PRV-2). */
public interface ProviderOpenedIEHandler {

    void handle(ProviderOpenedIE event);
}
