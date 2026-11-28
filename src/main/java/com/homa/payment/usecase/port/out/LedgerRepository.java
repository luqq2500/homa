package com.homa.payment.usecase.port.out;

import com.homa.payment.domain.LedgerEntry;

/**
 * Payment's books (PAY-4): an append-only list of double-entry lines. Entries are posted in the transaction of the use
 * case that moves the money and are never changed or removed.
 */
public interface LedgerRepository {

    /** Appends the entry to the ledger. */
    void post(LedgerEntry entry);
}
