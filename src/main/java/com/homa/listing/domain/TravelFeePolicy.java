package com.homa.listing.domain;

import com.homa.kernel.Money;
import com.homa.listing.domain.exception.InvalidTravelFeeException;
import com.homa.listing.domain.exception.UnpricedCityException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** The provider's travel fee: none, flat, or by city (OFR-3). */
public sealed interface TravelFeePolicy {

    /** The travel fee for a reservation at an address in the city. */
    Money feeFor(City city);

    /** The covered cities this policy has no price for; a listing is valid only when this is empty. */
    Set<City> unpricedAmong(Set<City> covered);

    static TravelFeePolicy none() {
        return new None();
    }

    static TravelFeePolicy flat(Money fee) {
        return new Flat(fee);
    }

    static TravelFeePolicy byCity(Map<City, Money> fees) {
        return new ByCity(fees);
    }

    /** No travel fee anywhere. */
    record None() implements TravelFeePolicy {

        @Override
        public Money feeFor(City city) {
            return Money.zero(Money.MYR);
        }

        @Override
        public Set<City> unpricedAmong(Set<City> covered) {
            return Set.of();
        }
    }

    /** The same fee in every covered city: Kemas Home's RM20. */
    record Flat(Money fee) implements TravelFeePolicy {

        public Flat {
            requireNotNegative(fee);
        }

        @Override
        public Money feeFor(City city) {
            return fee;
        }

        @Override
        public Set<City> unpricedAmong(Set<City> covered) {
            return Set.of();
        }
    }

    /** A fee per city, which must price every covered city: RM10 for Salak Tinggi, RM25 for Putrajaya. */
    record ByCity(Map<City, Money> fees) implements TravelFeePolicy {

        public ByCity {
            fees = Map.copyOf(fees);
            if (fees.isEmpty()) {
                throw new InvalidTravelFeeException("A by-city travel fee must price at least one city");
            }
            fees.values().forEach(TravelFeePolicy::requireNotNegative);
        }

        @Override
        public Money feeFor(City city) {
            Money fee = fees.get(city);
            if (fee == null) {
                throw new UnpricedCityException("No travel fee is set for " + city.code());
            }
            return fee;
        }

        @Override
        public Set<City> unpricedAmong(Set<City> covered) {
            return covered.stream().filter(city -> !fees.containsKey(city)).collect(Collectors.toUnmodifiableSet());
        }
    }

    private static void requireNotNegative(Money fee) {
        Objects.requireNonNull(fee, "fee");
        if (fee.isNegative()) {
            throw new InvalidTravelFeeException("A travel fee can't be negative, was " + fee);
        }
    }
}
