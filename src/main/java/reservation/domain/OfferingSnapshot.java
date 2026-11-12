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
 * @param supervisorRule when the provider sends a supervisor; empty when they set none
 * @param takesRequests false while the listing is paused because the provider's re-verification lapsed
 * @param acceptancePolicy whether the request waits for the provider or is accepted instantly once paid
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
        CancellationTier cancellationTier,
        Optional<SupervisorRule> supervisorRule,
        boolean takesRequests,
        AcceptancePolicy acceptancePolicy) {

    public OfferingSnapshot {
        providerId = ReservationText.require(providerId, "A provider id");
        Objects.requireNonNull(hourlyRatePerHelper);
        Objects.requireNonNull(separateTravelFee);
        jobs = Set.copyOf(jobs);
        cities = Set.copyOf(cities);
        Objects.requireNonNull(cancellationTier);
        Objects.requireNonNull(supervisorRule);
        Objects.requireNonNull(acceptancePolicy);
    }

    boolean isInstantBook() {
        return acceptancePolicy == AcceptancePolicy.INSTANT_BOOK;
    }

    boolean requiresSupervisor(int helpers, int hours) {
        return supervisorRule.filter(rule -> rule.requiresSupervisor(helpers, hours)).isPresent();
    }

    boolean offersHours(int hours) {
        return hours >= minHours && hours <= maxHours;
    }

    boolean offersHelpers(int helpers) {
        return helpers >= minHelpers && helpers <= maxHelpers;
    }
}
