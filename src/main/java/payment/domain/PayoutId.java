package payment.domain;

import java.util.UUID;

/** Type-safe generator for Payout ids. */
public final class PayoutId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
