package com.homa.reservation.domain.replica;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * The part of a provider that Listing publishes in full on every change (OFR-1 to OFR-6, OFR-9, OFR-10): the cities
 * and jobs it covers, the terms it offers today, its booking bounds, minimum notice and buffer. Reservations already
 * made keep their own terms (RES-8).
 */
public record ProviderListing(
        Set<String> cities,
        Set<String> jobs,
        OfferedTerms terms,
        BookingBounds bounds,
        Duration minimumNotice,
        Duration buffer) {

    public ProviderListing {
        cities = Set.copyOf(cities);
        jobs = Set.copyOf(jobs);
        Objects.requireNonNull(terms, "terms");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(minimumNotice, "minimumNotice");
        Objects.requireNonNull(buffer, "buffer");
    }
}
