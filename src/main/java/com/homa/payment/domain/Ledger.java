package com.homa.payment.domain;

import com.homa.kernel.Money;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Payment's books, in minimal double entry (PAY-4): every {@link LedgerEntry} debits one account and credits another by
 * the same amount, so what the debit-side accounts hold always equals what the credit-side accounts hold. The
 * accounts are what Homa holds for customers, owes providers, earns, pays out as compensation and is owed in fines.
 * Entries are only added, never changed or removed.
 */
public final class Ledger {

    private final List<LedgerEntry> entries = new ArrayList<>();

    /** Books that start with nothing posted. */
    public Ledger() {
    }

    /** Rebuilds the books from every entry posted so far, oldest first. */
    public static Ledger restore(List<LedgerEntry> entries) {
        Ledger ledger = new Ledger();
        entries.forEach(ledger::post);
        return ledger;
    }

    /** Adds the entry: its amount is debited to one account and credited to another. */
    public void post(LedgerEntry entry) {
        entries.add(Objects.requireNonNull(entry, "entry"));
    }

    /** What the account holds after every entry posted so far; zero when nothing touched it. */
    public Money balanceOf(LedgerAccount account) {
        Objects.requireNonNull(account, "account");
        return account.balanceOf(total(entry -> entry.movement().debited() == account),
                total(entry -> entry.movement().credited() == account));
    }

    /**
     * What Homa keeps: its platform fees and fines, less the compensation it paid out (PAY-4). After a provider no
     * show, RM105 fine less RM20 compensation is RM85.
     */
    public Money homaKeeps() {
        return balanceOf(LedgerAccount.HOMA_REVENUE).minus(balanceOf(LedgerAccount.HOMA_COMPENSATION_EXPENSE));
    }

    /** Every entry posted, oldest first. */
    public List<LedgerEntry> entries() {
        return List.copyOf(entries);
    }

    private Money total(Predicate<LedgerEntry> touchingTheAccount) {
        return entries.stream().filter(touchingTheAccount).map(LedgerEntry::amount)
                .reduce(Money.zero(Money.MYR), Money::plus);
    }
}
