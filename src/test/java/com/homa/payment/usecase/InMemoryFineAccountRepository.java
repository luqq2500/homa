package com.homa.payment.usecase;

import com.homa.payment.domain.ProviderFineAccount;
import com.homa.payment.domain.ProviderId;
import com.homa.payment.usecase.port.out.FineAccountRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Test only: fine accounts by provider, and every save in order, so a test can tell a change was saved. */
public class InMemoryFineAccountRepository implements FineAccountRepository {

    private final Map<ProviderId, ProviderFineAccount> accounts = new LinkedHashMap<>();
    private final List<ProviderFineAccount> saves = new ArrayList<>();

    /** Puts an account in place without counting it as a save. */
    public void given(ProviderFineAccount account) {
        accounts.put(account.providerId(), account);
    }

    @Override
    public Optional<ProviderFineAccount> findByProviderId(ProviderId providerId) {
        return Optional.ofNullable(accounts.get(providerId));
    }

    @Override
    public void save(ProviderFineAccount account) {
        accounts.put(account.providerId(), account);
        saves.add(account);
    }

    public List<ProviderFineAccount> saves() {
        return List.copyOf(saves);
    }
}
