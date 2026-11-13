package reservation.domain;

import java.util.Set;
import kernel.City;
import kernel.Money;

/**
 * A provider's offering as a customer can request it right now. A request keeps the terms it was
 * made with; a later change of the offering applies to new requests only.
 */
public record ProviderTerms(
        String providerId,
        boolean live,
        Set<String> jobIds,
        int minHelpers,
        int maxHelpers,
        SupervisorRule supervisorRule,
        Money hourlyRate,
        boolean travelFeeSeparate,
        Money travelFee,
        int minHours,
        int maxHours,
        Set<City> cities,
        CancellationTier cancellationTier,
        AcceptancePolicy acceptancePolicy) {

    public ProviderTerms {
        jobIds = Set.copyOf(jobIds);
        cities = Set.copyOf(cities);
    }

    public boolean usesInstantBook() {
        return acceptancePolicy == AcceptancePolicy.INSTANT_BOOK;
    }
}
