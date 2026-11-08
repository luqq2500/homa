package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** An invited helper's eKYC came back rejected; the helper stays off the team and can redo eKYC. */
public record HelperInviteRejectedIE(
        String helperId,
        String providerId,
        Instant occurredAt) implements AccountIE {

    public HelperInviteRejectedIE {
        Objects.requireNonNull(helperId, "helperId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
