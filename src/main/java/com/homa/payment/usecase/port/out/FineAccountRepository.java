package com.homa.payment.usecase.port.out;

import com.homa.payment.domain.ProviderFineAccount;
import com.homa.payment.domain.ProviderId;
import java.util.Optional;

/** Stores what each provider owes in fines (PAY-4, PAY-5), one account per provider. */
public interface FineAccountRepository {

    /** Empty until the provider has been fined once. */
    Optional<ProviderFineAccount> findByProviderId(ProviderId providerId);

    /** Inserts or updates the provider's account, with every fine on it, cleared ones too. */
    void save(ProviderFineAccount account);
}
