package com.homa.catalogue.domain;

import java.util.Set;

/**
 * The cities Homa serves. Providers cover cities from this area, and search only shows providers who
 * cover the city of the customer's address.
 */
public final class ServiceArea {

    private static final Set<City> LAUNCH_CITIES =
            Set.of(City.of("Salak Tinggi"), City.of("Cyberjaya"), City.of("Putrajaya"));

    private final String id;
    private final Set<City> cities;

    private ServiceArea(String id, Set<City> cities) {
        this.id = id;
        this.cities = Set.copyOf(cities);
    }

    /** Homa's launch area: Salak Tinggi, Cyberjaya and Putrajaya. */
    public static ServiceArea launch() {
        return new ServiceArea(new ServiceAreaId().generate(), LAUNCH_CITIES);
    }

    public boolean covers(City city) {
        return cities.contains(city);
    }

    public Set<City> cities() {
        return cities;
    }

    public String id() {
        return id;
    }
}
