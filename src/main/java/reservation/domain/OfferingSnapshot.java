package reservation.domain;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import kernel.Money;

/**
 * The provider's offering as the reservation copied it at request time. Later changes to the provider's
 * listing do not change it.
 *
 * @param separateTravelFee the flat travel fee when charged separately; empty when travel is included
 */
public record OfferingSnapshot(
        String providerId,
        Money hourlyRatePerHelper,
        Optional<Money> separateTravelFee,
        int minHours,
        int maxHours,
        int minHelpers,
        int maxHelpers,
        Set<JobCode> jobs,
        Set<City> cities,
        CancellationTier cancellationTier) {

    public OfferingSnapshot {
        providerId = ReservationText.require(providerId, "A provider id");
        Objects.requireNonNull(hourlyRatePerHelper);
        Objects.requireNonNull(separateTravelFee);
        jobs = Set.copyOf(jobs);
        cities = Set.copyOf(cities);
        Objects.requireNonNull(cancellationTier);
    }

    boolean offersHours(int hours) {
        return hours >= minHours && hours <= maxHours;
    }

    boolean offersHelpers(int helpers) {
        return helpers >= minHelpers && helpers <= maxHelpers;
    }
}
