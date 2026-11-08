package account.domain;

import java.util.UUID;

/** Type-safe generator for PhoneVerification ids. */
public final class PhoneVerificationId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
