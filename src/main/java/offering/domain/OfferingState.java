package offering.domain;

import java.util.Set;
import kernel.City;
import kernel.Money;

/**
 * Everything an offering holds, as plain data: what is stored, viewed and announced. Parts not set
 * yet are null; {@code stoppedBecause} is set only while the listing is offline or paused.
 */
public record OfferingState(
        String providerId,
        Listing listing,
        StopReason stoppedBecause,
        ProviderProfile profile,
        Set<String> jobIds,
        Set<String> toolIds,
        HelperRange helpers,
        SupervisorRule supervisorRule,
        Money hourlyRate,
        TravelFee travelFee,
        HoursRange hours,
        Set<City> cities,
        CancellationTier cancellationTier,
        AcceptancePolicy acceptancePolicy,
        InsuranceDeclaration insurance,
        boolean businessVerified,
        boolean payoutAccountAdded,
        Team team) {

    public OfferingState {
        jobIds = Set.copyOf(jobIds);
        toolIds = Set.copyOf(toolIds);
        cities = Set.copyOf(cities);
    }
}
