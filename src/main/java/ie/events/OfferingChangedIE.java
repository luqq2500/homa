package ie.events;

import ie.port.OfferingIE;
import java.time.Instant;
import java.util.Set;
import kernel.City;
import kernel.Money;

/** A provider's offering as it now stands. It applies to new requests only. Parts not set yet are null, or 0 for numbers; a supervisor threshold of 0 is not set. */
public record OfferingChangedIE(String providerId, String listing, String profileName, String profilePicture, Set<String> jobIds, Set<String> toolIds, int minHelpers, int maxHelpers, int supervisorFromHelpers, int supervisorFromHours, Money hourlyRate, boolean travelFeeSeparate, Money travelFee, int minHours, int maxHours, Set<City> cities, String cancellationTier, String acceptancePolicy, String insurer, Instant insuredUntil, Instant occurredAt) implements OfferingIE {

    public OfferingChangedIE {
        jobIds = Set.copyOf(jobIds);
        toolIds = Set.copyOf(toolIds);
        cities = Set.copyOf(cities);
    }
}
