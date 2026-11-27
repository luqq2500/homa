package com.homa.payment.usecase.dto;

/**
 * A provider's owner submits its payout account (PRV-2).
 *
 * @param actingPersonId    the person submitting it, filled from the token by infra/security; only the provider's
 *                          owner may (PRV-1)
 * @param providerId        the provider whose payout account it is
 * @param holder            OWNER or BUSINESS
 * @param accountHolderName the name the bank account is held in
 */
public record SubmitPayoutAccountRequest(String actingPersonId, String providerId, String holder, String bank,
        String accountNumber, String accountHolderName) {

    /** Never writes the account number out, so a request cannot leak it into a log. */
    @Override
    public String toString() {
        return "SubmitPayoutAccountRequest[" + actingPersonId + ", " + providerId + ", " + holder + ", " + bank + "]";
    }
}
