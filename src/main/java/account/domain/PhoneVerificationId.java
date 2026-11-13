package account.domain;

import java.util.UUID;

/** Generates the id of a phone \1erification. */
public final class PhoneVerificationId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
