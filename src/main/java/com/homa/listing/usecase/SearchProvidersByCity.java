package com.homa.listing.usecase;

import com.homa.listing.domain.City;
import com.homa.listing.domain.ProviderListing;
import com.homa.listing.domain.ProviderStanding;
import com.homa.listing.usecase.dto.SearchProvidersByCityRequest;
import com.homa.listing.usecase.dto.SearchProvidersByCityResponse;
import com.homa.listing.usecase.dto.SearchProvidersByCityResponse.FoundProvider;
import com.homa.listing.usecase.port.in.ProviderSearcher;
import com.homa.listing.usecase.port.out.ListingRepository;
import com.homa.listing.usecase.port.out.ProviderStandingRepository;
import java.util.List;

/**
 * RES-1, minimal: the providers covering the customer's city that are live and bookable (PRV-2), with their rate
 * and their travel fee to that city. Ratings, reliability and the insurance tag join in later slices.
 */
public final class SearchProvidersByCity implements ProviderSearcher {

    private final ListingRepository listings;
    private final ProviderStandingRepository standings;

    public SearchProvidersByCity(ListingRepository listings, ProviderStandingRepository standings) {
        this.listings = listings;
        this.standings = standings;
    }

    @Override
    public SearchProvidersByCityResponse search(SearchProvidersByCityRequest request) {
        City city = new City(request.city());
        List<FoundProvider> found = listings.findCovering(city).stream()
                .filter(this::isListed)
                .map(listing -> new FoundProvider(listing.providerId().value(),
                        listing.terms().rate().perHelperPerHour(), listing.terms().travelFee().feeFor(city)))
                .toList();
        return new SearchProvidersByCityResponse(request.city(), found);
    }

    private boolean isListed(ProviderListing listing) {
        return standings.findById(listing.providerId()).map(ProviderStanding::isListed).orElse(false);
    }
}
