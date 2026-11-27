package com.homa.verification.usecase.port.out;

import com.homa.verification.domain.IdentityNumber;
import com.homa.verification.domain.PersonName;

/**
 * The payment provider's account-holder check (§11, PRV-2, IDN-3). Verification alone holds identity numbers, so
 * it asks. The client names the payout account by {@code gatewayAccountRef}, the payment provider's own reference
 * for the account Payment registered, so the bank account number never reaches Verification. It sends the person's
 * identity number with it and returns only whether the account is theirs. The payment provider matches on the
 * identity number, or on the name where the bank gives no number. The identity number goes to the payment provider
 * and nowhere else: never into a log, an event or a response.
 */
public interface AccountHolderCheck {

    /** True when the payment provider confirms the account it knows by this reference is held by this person. */
    boolean isHeldBy(String gatewayAccountRef, IdentityNumber identityNumber, PersonName name);
}
