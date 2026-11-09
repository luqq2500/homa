package ie.events;

import ie.port.OfferingIE;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import kernel.Money;

/** A live listing changed. It carries the whole listing as it now is. When travel is included, {@code travelFee} is RM0.00. {@code supervisorFromHelpers} and {@code supervisorFromHours} are 0 when that threshold is not set. */
public record ListingChangedIE(
        String listingId,
        String providerId,
        Set<String> jobCodes,
        Set<String> toolCodes,
        Money hourlyRatePerHelper,
        boolean travelIncluded,
        Money travelFee,
        int minHours,
        int maxHours,
        int minHelpers,
        int maxHelpers,
        Set<String> cities,
        String cancellationTier,
        String providerType,
        Set<String> helperIds,
        Set<String> supervisorIds,
        int supervisorFromHelpers,
        int supervisorFromHours,
        Instant occurredAt) implements OfferingIE {

    public ListingChangedIE {
        Objects.requireNonNull(listingId, "listingId");
        Objects.requireNonNull(providerId, "providerId");
        jobCodes = Set.copyOf(jobCodes);
        toolCodes = Set.copyOf(toolCodes);
        Objects.requireNonNull(hourlyRatePerHelper, "hourlyRatePerHelper");
        Objects.requireNonNull(travelFee, "travelFee");
        cities = Set.copyOf(cities);
        Objects.requireNonNull(cancellationTier, "cancellationTier");
        Objects.requireNonNull(providerType, "providerType");
        helperIds = Set.copyOf(helperIds);
        supervisorIds = Set.copyOf(supervisorIds);
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
