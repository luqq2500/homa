package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** The KYC provider's monitoring found a serious flag on a business, such as being struck off or a sanctions match: it lapses at once, and its upcoming accepted reservations are disputed and cancelled with a full refund. */
public record ProviderFlaggedIE(
        String providerId,
        String reason,
        Instant occurredAt) implements AccountIE {

    public ProviderFlaggedIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
