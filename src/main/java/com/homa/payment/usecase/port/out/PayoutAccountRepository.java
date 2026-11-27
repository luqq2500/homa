package com.homa.payment.usecase.port.out;

import com.homa.payment.domain.PayoutAccount;
import com.homa.payment.domain.ProviderId;
import java.util.Optional;

/** Stores each provider's payout account (PRV-2), one per provider. */
public interface PayoutAccountRepository {

    Optional<PayoutAccount> findByProviderId(ProviderId providerId);

    /** Inserts or updates the provider's payout account. */
    void save(PayoutAccount account);
}
