package com.homa.payment.domain;

import com.homa.kernel.Money;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The money lines of a released payout: what the provider is owed, Homa's platform fee taken from every payout
 * (PAY-3), the fine deducted after it (PAY-5), and what is sent. Payout RM200: fee RM31, RM169 after the fee. A RM45
 * cancellation fee: fee RM6.98, RM38.02 after the fee. A RM300 payout from a provider owing RM105: fee RM46.50,
 * RM253.50 after the fee, RM50.70 deducted, RM202.80 sent. The lines always add up: gross is the platform fee, plus
 * the fine deducted, plus what is sent.
 */
public record PayoutRelease(PayoutId payoutId, ProviderId providerId, PayoutKind kind, Money gross,
        Money platformFee, Money fineDeduction, Instant releasedAt) {

    public PayoutRelease {
        Objects.requireNonNull(payoutId, "payoutId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(gross, "gross");
        Objects.requireNonNull(platformFee, "platformFee");
        Objects.requireNonNull(fineDeduction, "fineDeduction");
        Objects.requireNonNull(releasedAt, "releasedAt");
        if (fineDeduction.isNegative() || fineDeduction.isGreaterThan(gross.minus(platformFee))) {
            throw new IllegalArgumentException("A fine deduction is from nothing up to the payout after the platform "
                    + "fee, was " + fineDeduction);
        }
    }

    /** Gross minus the rounded platform fee, so the lines always add up (PAY-8). The base for a fine deduction (PAY-5). */
    public Money afterPlatformFee() {
        return gross.minus(platformFee);
    }

    /** What is sent to the provider: the payout after the platform fee, less the fine deducted (PAY-5). */
    public Money net() {
        return afterPlatformFee().minus(fineDeduction);
    }

    /**
     * The ledger entries this release posts (PAY-4): the held money becomes owed to the provider, Homa takes its
     * platform fee, the fine is deducted, and the rest is sent. A line of nothing posts nothing.
     */
    public List<LedgerEntry> ledgerEntries() {
        List<LedgerEntry> entries = new ArrayList<>();
        addIfPositive(entries, LedgerMovement.PAYOUT_RELEASED, gross);
        addIfPositive(entries, LedgerMovement.PLATFORM_FEE_TAKEN, platformFee);
        addIfPositive(entries, LedgerMovement.FINE_DEDUCTED, fineDeduction);
        addIfPositive(entries, LedgerMovement.PAYOUT_SENT, net());
        return entries;
    }

    private void addIfPositive(List<LedgerEntry> entries, LedgerMovement movement, Money amount) {
        if (amount.isPositive()) {
            entries.add(new LedgerEntry(movement, amount, releasedAt));
        }
    }
}
