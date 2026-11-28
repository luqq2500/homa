package com.homa.payment.domain;

import com.homa.kernel.Money;
import java.util.Objects;

/**
 * The accounts of Payment's ledger (PAY-4). Every money movement debits one account and credits another by the same
 * amount, so the books always balance. A debit-side account (what Homa holds or is owed, and what it spends) grows by
 * debits; a credit-side account (what Homa owes others, and what it earns) grows by credits.
 */
public enum LedgerAccount {
    /** Homa's money with the payment provider: what came in from customers and what has gone out. Debit side. */
    HOMA_BANK(true),
    /** Fines the providers still owe, until their payouts clear them (PAY-4, PAY-5). Debit side. */
    FINE_RECEIVABLE(true),
    /** What Homa paid customers as compensation, from its own money (PAY-4). Debit side. */
    HOMA_COMPENSATION_EXPENSE(true),
    /** Customers' money Homa holds, until it is refunded or released to the provider (PAY-1, PAY-4). Credit side. */
    CUSTOMER_HELD(false),
    /** What Homa owes providers: a released payout, until the platform fee, fine and the rest leave it. Credit side. */
    PROVIDER_PAYABLE(false),
    /** What Homa keeps: platform fees and fines (PAY-3, PAY-4). Credit side. */
    HOMA_REVENUE(false);

    private final boolean debitSide;

    LedgerAccount(boolean debitSide) {
        this.debitSide = debitSide;
    }

    /** What the account holds, given everything debited to it and everything credited to it. */
    Money balanceOf(Money debited, Money credited) {
        Objects.requireNonNull(debited, "debited");
        Objects.requireNonNull(credited, "credited");
        return debitSide ? debited.minus(credited) : credited.minus(debited);
    }
}
