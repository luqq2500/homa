package payment.domain;

import java.util.UUID;

/** Type-safe generator for Fine ids. */
public final class FineId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
