package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A provider invited a helper; the helper is not on the team until their eKYC is verified. */
public record HelperInvitedIE(
        String helperId,
        String providerId,
        Instant occurredAt) implements AccountIE {

    public HelperInvitedIE {
        Objects.requireNonNull(helperId, "helperId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
