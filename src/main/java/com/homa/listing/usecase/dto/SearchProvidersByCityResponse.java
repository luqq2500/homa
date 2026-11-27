package com.homa.listing.usecase.dto;

import com.homa.kernel.Money;
import java.util.List;

/** The live, bookable providers covering the city (RES-1, PRV-2). */
public record SearchProvidersByCityResponse(String city, List<FoundProvider> providers) {

    public SearchProvidersByCityResponse {
        providers = List.copyOf(providers);
    }

    /**
     * @param hourlyRatePerHelper the provider's rate before any discount (OFR-2)
     * @param travelFee           the provider's travel fee to the searched city (OFR-3)
     */
    public record FoundProvider(String providerId, Money hourlyRatePerHelper, Money travelFee) {
    }
}
