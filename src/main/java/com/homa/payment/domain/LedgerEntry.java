package com.homa.payment.domain;

import com.homa.kernel.Money;
import java.time.Instant;
import java.util.Objects;

/**
 * One line of the ledger: a movement and its amount. The amount is debited to the movement's debited account and
 * credited to its credited account (PAY-4), so no entry can unbalance the books. Like every amount Payment moves it is
 * positive and already rounded to the sen (PAY-8).
 */
public record LedgerEntry(LedgerMovement movement, Money amount, Instant postedAt) {

    public LedgerEntry {
        Objects.requireNonNull(movement, "movement");
        Amounts.requirePayable(amount, "A ledger entry");
        Objects.requireNonNull(postedAt, "postedAt");
    }
}
