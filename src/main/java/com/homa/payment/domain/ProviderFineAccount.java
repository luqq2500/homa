package com.homa.payment.domain;

import com.homa.kernel.Money;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * What one provider owes in fines (PAY-4, PAY-5). A fine is recorded when a reservation is cancelled against the
 * provider, with the reservation it came from. Fines are deducted from the provider's payouts after the platform fee,
 * oldest first, at most the fine deduction cap of each payout (20%), whatever the payout kind; whatever is left carries
 * over to the next payout until the account is cleared.
 *
 * <p>For now the account is recognised by the provider's id. Matching a provider that closes and rejoins by its owner
 * or registration number (PAY-9) comes with the closing rules.
 */
public final class ProviderFineAccount {

    private final ProviderId providerId;
    private final List<Fine> fines = new ArrayList<>();

    private ProviderFineAccount(ProviderId providerId) {
        this.providerId = Objects.requireNonNull(providerId, "providerId");
    }

    /** A provider that owes nothing yet. */
    public static ProviderFineAccount openFor(ProviderId providerId) {
        return new ProviderFineAccount(providerId);
    }

    /**
     * Rebuilds the account from what was stored: every fine, cleared ones too, in the order they were recorded. Each
     * fine checks its own shape.
     */
    public static ProviderFineAccount restore(ProviderId providerId, List<Fine> fines) {
        ProviderFineAccount account = new ProviderFineAccount(providerId);
        account.fines.addAll(fines);
        return account;
    }

    /** The provider is fined for a reservation (PAY-4): it owes the whole fine, after every fine before it. */
    public void recordFine(ReservationId reservationId, Money amount, Instant recordedAt) {
        fines.add(Fine.imposed(reservationId, amount, recordedAt));
    }

    /**
     * Takes the fine out of a payout about to be sent (PAY-5): at most the fine deduction cap in force when the
     * reservation was Requested (P3), 20% of the payout after the platform fee, rounded once to the sen (PAY-8), and
     * never more than the provider owes. It clears the oldest fines first and returns what was deducted.
     */
    public Money deductFrom(Money payoutAfterPlatformFee, Instant requestedAt) {
        return deductFrom(payoutAfterPlatformFee, PaymentValues.asOf(requestedAt));
    }

    Money deductFrom(Money payoutAfterPlatformFee, PaymentValues.Version values) {
        Objects.requireNonNull(payoutAfterPlatformFee, "payoutAfterPlatformFee");
        if (payoutAfterPlatformFee.isNegative()) {
            throw new IllegalArgumentException("A payout after the platform fee is not negative, was "
                    + payoutAfterPlatformFee);
        }
        Money deduction = values.fineDeductionCap().of(payoutAfterPlatformFee).roundedToSen().min(outstanding());
        Money toClear = deduction;
        for (int i = 0; i < fines.size(); i++) {
            Money cleared = fines.get(i).outstanding().min(toClear);
            fines.set(i, fines.get(i).clearedBy(cleared));
            toClear = toClear.minus(cleared);
        }
        return deduction;
    }

    public ProviderId providerId() {
        return providerId;
    }

    /** Everything the provider still owes in fines. */
    public Money outstanding() {
        return fines.stream().map(Fine::outstanding).reduce(Money.zero(Money.MYR), Money::plus);
    }

    /** Every fine in the order it was recorded, oldest first; cleared ones stay (DAT-6). */
    public List<Fine> fines() {
        return List.copyOf(fines);
    }
}
