package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A helper's eKYC came back rejected; the provider must have the helper redo eKYC. */
public record HelperVerificationRejectedIE(
        String helperId,
        String providerId,
        Instant occurredAt) implements AccountIE {

    public HelperVerificationRejectedIE {
        Objects.requireNonNull(helperId, "helperId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
