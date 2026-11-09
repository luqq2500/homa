package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** Homa checked a business provider's SSM registration. */
public record BusinessRegistrationCheckedIE(
        String providerId,
        String ssmNumber,
        Instant occurredAt) implements AccountIE {

    public BusinessRegistrationCheckedIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(ssmNumber, "ssmNumber");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
