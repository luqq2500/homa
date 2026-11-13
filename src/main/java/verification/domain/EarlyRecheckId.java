package verification.domain;

import java.util.UUID;

/** Generates the id of an early \1echeck. */
public final class EarlyRecheckId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
