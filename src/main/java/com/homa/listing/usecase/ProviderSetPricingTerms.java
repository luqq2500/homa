package com.homa.listing.usecase;

import com.homa.kernel.Percentage;
import com.homa.listing.domain.DiscountRule;
import com.homa.listing.domain.HourlyRate;
import com.homa.listing.usecase.dto.ProviderSetPricingTermsRequest;
import com.homa.listing.usecase.dto.ProviderSetPricingTermsResponse;
import com.homa.listing.usecase.port.in.PricingTermsSetter;
import com.homa.listing.usecase.port.out.ListingIEPublisher;
import com.homa.listing.usecase.port.out.ListingOwners;
import com.homa.listing.usecase.port.out.ListingRepository;
import java.time.Duration;

/**
 * OFR-2 to OFR-4: the rate, travel fee and discount, set together so the minimum rate (OFR-11) is checked against
 * the result and not against the order of the changes. Publishes the full terms.
 */
public final class ProviderSetPricingTerms implements PricingTermsSetter {

    private final ListingChanges changes;

    public ProviderSetPricingTerms(ListingRepository listings, ListingOwners owners, ListingIEPublisher publisher,
                                   ListingEvents events) {
        this.changes = new ListingChanges(listings, owners, publisher, events);
    }

    @Override
    public ProviderSetPricingTermsResponse set(ProviderSetPricingTermsRequest request) {
        changes.apply(request.actingPersonId(), request.providerId(), listing -> listing.setPricingTerms(
                new HourlyRate(request.hourlyRatePerHelper()),
                TravelFeeKind.valueOf(request.travelFeeKind()).policy(request.flatTravelFee(), request.travelFeeByCity()),
                discount(request)));
        return new ProviderSetPricingTermsResponse(request.providerId());
    }

    private static DiscountRule discount(ProviderSetPricingTermsRequest request) {
        return request.discountPercent() == null
                ? DiscountRule.none()
                : DiscountRule.of(Percentage.of(request.discountPercent()),
                        Duration.ofMinutes(request.discountMinMinutes()), request.discountMinHelpers());
    }
}
