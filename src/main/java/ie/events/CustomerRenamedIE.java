package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A customer changed their name; chats show the new one. */
public record CustomerRenamedIE(
        String customerId,
        String name,
        Instant occurredAt) implements AccountIE {

    public CustomerRenamedIE {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
