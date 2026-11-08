package ie.events;

import ie.port.OfferingIE;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import kernel.Money;

/** A provider's listing went live, with what search needs to match it. */
public record ListingWentLiveIE(
        String listingId,
        String providerId,
        Set<String> jobCodes,
        Set<String> toolCodes,
        Money hourlyRatePerHelper,
        Optional<Money> separateTravelFee,
        int minHours,
        int maxHours,
        int minHelpers,
        int maxHelpers,
        Set<String> cities,
        String cancellationTier,
        Set<String> verifiedHelperIds,
        Instant occurredAt) implements OfferingIE {

    public ListingWentLiveIE {
        Objects.requireNonNull(listingId, "listingId");
        Objects.requireNonNull(providerId, "providerId");
        jobCodes = Set.copyOf(jobCodes);
        toolCodes = Set.copyOf(toolCodes);
        Objects.requireNonNull(hourlyRatePerHelper, "hourlyRatePerHelper");
        Objects.requireNonNull(separateTravelFee, "separateTravelFee");
        cities = Set.copyOf(cities);
        Objects.requireNonNull(cancellationTier, "cancellationTier");
        verifiedHelperIds = Set.copyOf(verifiedHelperIds);
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
