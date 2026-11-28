package com.homa.payment.domain;

import static com.homa.payment.domain.LedgerAccount.CUSTOMER_HELD;
import static com.homa.payment.domain.LedgerAccount.FINE_RECEIVABLE;
import static com.homa.payment.domain.LedgerAccount.HOMA_BANK;
import static com.homa.payment.domain.LedgerAccount.HOMA_COMPENSATION_EXPENSE;
import static com.homa.payment.domain.LedgerAccount.HOMA_REVENUE;
import static com.homa.payment.domain.LedgerAccount.PROVIDER_PAYABLE;

/**
 * Each way money moves in Payment, and the two accounts it moves between: one debited, one credited, by the same
 * amount. Homa pays compensation from its own money and recovers it through the fine, so Homa keeps the fine less the
 * compensation (PAY-4): RM105 fine, RM20 compensation, RM85 kept.
 */
public enum LedgerMovement {
    /** The customer's payment is held, once the reservation is Requested (PAY-1). */
    PAYMENT_HELD(HOMA_BANK, CUSTOMER_HELD),
    /** Part or all of what is held is refunded to the customer (PAY-4, PAY-7). */
    REFUND_SENT(CUSTOMER_HELD, HOMA_BANK),
    /** Homa pays the customer compensation on top of a full refund (PAY-4). */
    COMPENSATION_PAID(HOMA_COMPENSATION_EXPENSE, HOMA_BANK),
    /** A fine on the provider: owed to Homa, which keeps it (PAY-4). */
    FINE_IMPOSED(FINE_RECEIVABLE, HOMA_REVENUE),
    /** A payout is released: the customer's held money becomes owed to the provider (PAY-2, PAY-6). */
    PAYOUT_RELEASED(CUSTOMER_HELD, PROVIDER_PAYABLE),
    /** Homa's platform fee comes off the released payout (PAY-3). */
    PLATFORM_FEE_TAKEN(PROVIDER_PAYABLE, HOMA_REVENUE),
    /** Part of the provider's fine is cleared from the released payout (PAY-5). */
    FINE_DEDUCTED(PROVIDER_PAYABLE, FINE_RECEIVABLE),
    /** What is left of the released payout is sent to the provider. */
    PAYOUT_SENT(PROVIDER_PAYABLE, HOMA_BANK);

    private final LedgerAccount debited;
    private final LedgerAccount credited;

    LedgerMovement(LedgerAccount debited, LedgerAccount credited) {
        this.debited = debited;
        this.credited = credited;
    }

    public LedgerAccount debited() {
        return debited;
    }

    public LedgerAccount credited() {
        return credited;
    }
}
