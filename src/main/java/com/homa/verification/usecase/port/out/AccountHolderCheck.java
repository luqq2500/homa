package com.homa.verification.usecase.port.out;

import com.homa.verification.domain.IdentityNumber;
import com.homa.verification.domain.PersonName;

/**
 * The payment provider's account-holder check (§11, PRV-2, IDN-3). Verification alone holds identity numbers, so
 * it asks. The client reads the payout account's holder name and account number from the payment provider by
 * {@code payoutAccountId}, sends them with the person's identity number to the check, and returns only whether the
 * account is theirs. It matches on the identity number, or on the name where the bank gives no number. The number
 * goes to the payment provider and nowhere else: never into a log, an event or a response.
 */
public interface AccountHolderCheck {

    /** True when the payment provider confirms the payout account is held by the person with this identity. */
    boolean isHeldBy(String payoutAccountId, IdentityNumber identityNumber, PersonName name);
}
