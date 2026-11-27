package com.homa.listing.usecase;

import com.homa.listing.domain.City;
import com.homa.listing.domain.ListingTerms;
import com.homa.listing.domain.ProviderId;
import com.homa.listing.domain.ProviderListing;
import com.homa.listing.usecase.port.out.ListingRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Stores each listing's terms, so a change that isn't saved is lost, as it would be in the database. */
final class InMemoryListingRepository implements ListingRepository {

    private final Map<ProviderId, ListingTerms> stored = new LinkedHashMap<>();

    @Override
    public Optional<ProviderListing> findById(ProviderId providerId) {
        return Optional.ofNullable(stored.get(providerId)).map(ProviderListing::rebuild);
    }

    @Override
    public List<ProviderListing> findCovering(City city) {
        return stored.values().stream()
                .filter(terms -> terms.cities().contains(city))
                .sorted(Comparator.comparing(terms -> terms.providerId().value()))
                .map(ProviderListing::rebuild)
                .toList();
    }

    @Override
    public void save(ProviderListing listing) {
        stored.put(listing.providerId(), listing.terms());
    }

    Optional<ListingTerms> termsOf(ProviderId providerId) {
        return Optional.ofNullable(stored.get(providerId));
    }
}
