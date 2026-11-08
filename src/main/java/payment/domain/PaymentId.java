package payment.domain;

import java.util.UUID;

/** Type-safe generator for Payment ids. */
public final class PaymentId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
