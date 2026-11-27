package com.homa.payment.usecase.dto;

/**
 * A provider submits its payout account (PRV-2).
 *
 * @param providerId        the acting provider, filled from the owner's token by infra/security
 * @param holder            OWNER or BUSINESS
 * @param accountHolderName the name the bank account is held in
 */
public record SubmitPayoutAccountRequest(String providerId, String holder, String bank, String accountNumber,
        String accountHolderName) {

    /** Never writes the account number out, so a request cannot leak it into a log. */
    @Override
    public String toString() {
        return "SubmitPayoutAccountRequest[" + providerId + ", " + holder + ", " + bank + "]";
    }
}
