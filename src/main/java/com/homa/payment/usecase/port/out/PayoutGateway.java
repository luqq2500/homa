package com.homa.payment.usecase.port.out;

import com.homa.kernel.Money;
import java.util.Objects;

/**
 * The payment provider, for providers' money. It registers a submitted payout account and names it by its own
 * reference, which is what Verification's account-holder check uses, so the bank account number stays in Payment
 * (PRV-2, IDN-3). It sends a released payout to the provider's verified payout account. "Sent" is what the provider
 * is told; arrival depends on the banks (EXT-3). Every call carries an idempotency key, so a repeated call never
 * registers or pays twice.
 */
public interface PayoutGateway {

    /** Registers the account with the payment provider and returns the provider's reference for it. */
    String registerAccount(AccountRegistration registration);

    void send(Transfer transfer);

    /**
     * One payout account to register. {@code payoutAccountId} is Homa's id for it, and the registration's
     * idempotency key.
     */
    record AccountRegistration(String payoutAccountId, String bank, String accountNumber, String accountHolderName) {

        public AccountRegistration {
            Objects.requireNonNull(payoutAccountId, "payoutAccountId");
            Objects.requireNonNull(bank, "bank");
            Objects.requireNonNull(accountNumber, "accountNumber");
            Objects.requireNonNull(accountHolderName, "accountHolderName");
        }

        /** Never writes the account number out, so a registration cannot leak it into a log. */
        @Override
        public String toString() {
            return "AccountRegistration[" + payoutAccountId + ", " + bank + "]";
        }
    }

    /** A payout transfer to one verified payout account (PRV-2). */
    record Transfer(String idempotencyKey, String payoutAccountId, String bank, String accountNumber,
            String accountHolderName, Money amount) {

        public Transfer {
            Objects.requireNonNull(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(payoutAccountId, "payoutAccountId");
            Objects.requireNonNull(bank, "bank");
            Objects.requireNonNull(accountNumber, "accountNumber");
            Objects.requireNonNull(accountHolderName, "accountHolderName");
            Objects.requireNonNull(amount, "amount");
        }

        /** Never writes the account number out in full, so a transfer cannot leak it into a log. */
        @Override
        public String toString() {
            return "Transfer[" + idempotencyKey + ", " + payoutAccountId + ", " + amount + "]";
        }
    }
}
