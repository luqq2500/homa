package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A customer registered with a confirmed phone number. */
public record CustomerRegisteredIE(
        String customerId,
        String name,
        String city,
        Instant occurredAt) implements AccountIE {

    public CustomerRegisteredIE {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
