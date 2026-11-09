package account.domain;

import java.util.UUID;

/** Type-safe generator for MemberVerification ids. */
public final class MemberVerificationId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
