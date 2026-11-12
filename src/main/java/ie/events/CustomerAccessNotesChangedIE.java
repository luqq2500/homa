package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A customer changed the access notes on their account; each new request copies them. */
public record CustomerAccessNotesChangedIE(
        String customerId,
        String notes,
        Instant occurredAt) implements AccountIE {

    public CustomerAccessNotesChangedIE {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(notes, "notes");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
