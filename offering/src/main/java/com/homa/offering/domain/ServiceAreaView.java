package com.homa.offering.domain;

import java.util.Objects;
import java.util.Set;

/** The cities Homa serves, as offering needs to know them: which cities a provider may cover. */
public record ServiceAreaView(Set<City> cities) {

    public ServiceAreaView {
        cities = Set.copyOf(Objects.requireNonNull(cities));
    }

    public static ServiceAreaView of(Set<City> cities) {
        return new ServiceAreaView(cities);
    }

    public boolean covers(City city) {
        return cities.contains(city);
    }
}
