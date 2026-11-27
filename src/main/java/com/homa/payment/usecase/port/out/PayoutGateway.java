package com.homa.payment.usecase.port.out;

import com.homa.kernel.Money;
import java.util.Objects;

/**
 * The payment provider, for providers' money: it sends a released payout to the provider's verified payout account.
 * "Sent" is what the provider is told; arrival depends on the banks (EXT-3). Every call carries an idempotency key,
 * so a repeated call never pays twice.
 */
public interface PayoutGateway {

    void send(Transfer transfer);

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
