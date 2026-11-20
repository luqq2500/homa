package com.homa.listing.domain;

import com.homa.listing.domain.exception.NothingOfferedException;
import java.util.Set;

/**
 * The jobs, tools and cities a provider covers (OFR-1). At least one job and one city, so a customer can select a
 * job (RES-2) and live in a covered city (RES-1); tools are optional.
 */
public record Offerings(Set<JobType> jobs, Set<Tool> tools, Set<City> cities) {

    public Offerings {
        jobs = Set.copyOf(jobs);
        tools = Set.copyOf(tools);
        cities = Set.copyOf(cities);
        if (jobs.isEmpty()) {
            throw new NothingOfferedException("Choose at least one job");
        }
        if (cities.isEmpty()) {
            throw new NothingOfferedException("Choose at least one city covered");
        }
    }

    public boolean covers(City city) {
        return cities.contains(city);
    }
}
