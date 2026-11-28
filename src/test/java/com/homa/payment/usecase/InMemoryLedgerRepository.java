package com.homa.payment.usecase;

import com.homa.kernel.Money;
import com.homa.payment.domain.Ledger;
import com.homa.payment.domain.LedgerAccount;
import com.homa.payment.domain.LedgerEntry;
import com.homa.payment.usecase.port.out.LedgerRepository;
import java.util.List;

/** Test only: the books, kept as the domain's {@link Ledger}, so a test can assert balances and entries. */
public class InMemoryLedgerRepository implements LedgerRepository {

    private final Ledger ledger = new Ledger();

    @Override
    public void post(LedgerEntry entry) {
        ledger.post(entry);
    }

    public List<LedgerEntry> entries() {
        return ledger.entries();
    }

    public Money balanceOf(LedgerAccount account) {
        return ledger.balanceOf(account);
    }

    public Money homaKeeps() {
        return ledger.homaKeeps();
    }
}
