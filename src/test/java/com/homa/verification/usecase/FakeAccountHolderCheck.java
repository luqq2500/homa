package com.homa.verification.usecase;

import com.homa.verification.domain.IdentityNumber;
import com.homa.verification.domain.PersonName;
import com.homa.verification.usecase.port.out.AccountHolderCheck;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The payment provider's account-holder check, answering from the accounts the test opened. */
public final class FakeAccountHolderCheck implements AccountHolderCheck {

    /** One question the check was asked. */
    public record Question(String gatewayAccountRef, IdentityNumber identityNumber, PersonName name) {
    }

    private final Map<String, IdentityNumber> holders = new HashMap<>();
    private final List<Question> asked = new ArrayList<>();
    private boolean down;

    /** The bank holds the account the payment provider knows as {@code gatewayAccountRef} for this identity. */
    public void accountHeldBy(String gatewayAccountRef, String identityNumber) {
        holders.put(gatewayAccountRef, new IdentityNumber(identityNumber));
    }

    public void goDown() {
        down = true;
    }

    @Override
    public boolean isHeldBy(String gatewayAccountRef, IdentityNumber identityNumber, PersonName name) {
        if (down) {
            throw new IllegalStateException("The payment provider is unavailable");
        }
        asked.add(new Question(gatewayAccountRef, identityNumber, name));
        return identityNumber.equals(holders.get(gatewayAccountRef));
    }

    public List<Question> asked() {
        return List.copyOf(asked);
    }
}
