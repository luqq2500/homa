package com.homa.payment.usecase;

import com.homa.payment.domain.PayoutAccount;
import com.homa.payment.domain.ProviderId;
import com.homa.payment.usecase.port.out.PayoutAccountRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Test only: payout accounts by provider, and every save in order. */
public class InMemoryPayoutAccountRepository implements PayoutAccountRepository {

    private final Map<ProviderId, PayoutAccount> accounts = new HashMap<>();
    private final List<PayoutAccount> saves = new ArrayList<>();

    /** Puts an account in place without counting it as a save. */
    public void given(PayoutAccount account) {
        accounts.put(account.providerId(), account);
    }

    @Override
    public Optional<PayoutAccount> findByProviderId(ProviderId providerId) {
        return Optional.ofNullable(accounts.get(providerId));
    }

    @Override
    public void save(PayoutAccount account) {
        accounts.put(account.providerId(), account);
        saves.add(account);
    }

    public List<PayoutAccount> saves() {
        return List.copyOf(saves);
    }
}
