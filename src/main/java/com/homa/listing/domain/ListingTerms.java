package com.homa.listing.domain;

import com.homa.listing.domain.exception.NoticeBelowFloorException;
import com.homa.listing.domain.exception.UnpricedCityException;
import java.util.Objects;
import java.util.Set;

/**
 * A full snapshot of a provider's listing terms: what {@code ListingTermsChangedIE} carries and what Reservation
 * freezes when a reservation is Requested (RES-8). Holds the rules that tie one setting to another, so no
 * snapshot can exist that breaks them:
 *
 * <ul>
 *   <li>a by-city travel fee prices every covered city (OFR-3);</li>
 *   <li>the minimum notice is at least the acceptance policy's floor (OFR-6);</li>
 *   <li>there is exactly one acceptance policy and one cancellation tier (OFR-9).</li>
 * </ul>
 */
public record ListingTerms(
        ProviderId providerId,
        Offerings offerings,
        HourlyRate rate,
        TravelFeePolicy travelFee,
        DiscountRule discount,
        BookingBounds bounds,
        MinimumNotice notice,
        JobBuffer buffer,
        AcceptancePolicy policy,
        CancellationTier tier) {

    public ListingTerms {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(offerings, "offerings");
        Objects.requireNonNull(rate, "rate");
        Objects.requireNonNull(travelFee, "travelFee");
        Objects.requireNonNull(discount, "discount");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(notice, "notice");
        Objects.requireNonNull(buffer, "buffer");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(tier, "tier");
        Set<City> unpriced = travelFee.unpricedAmong(offerings.cities());
        if (!unpriced.isEmpty()) {
            throw new UnpricedCityException("The travel fee has no price for " + unpriced);
        }
        if (!notice.allowedUnder(policy)) {
            throw new NoticeBelowFloorException("A " + policy + " provider's notice can't be under "
                    + policy.noticeFloor() + ", was " + notice.duration());
        }
    }

    /** The cities covered, which a customer's address must be in (RES-1). */
    public Set<City> cities() {
        return offerings.cities();
    }

    /** The jobs a customer can select from (RES-2). */
    public Set<JobType> jobs() {
        return offerings.jobs();
    }

    ListingTerms withOfferings(Offerings changed) {
        return new ListingTerms(providerId, changed, rate, travelFee, discount, bounds, notice, buffer, policy, tier);
    }

    ListingTerms withRate(HourlyRate changed) {
        return new ListingTerms(providerId, offerings, changed, travelFee, discount, bounds, notice, buffer, policy, tier);
    }

    ListingTerms withTravelFee(TravelFeePolicy changed) {
        return new ListingTerms(providerId, offerings, rate, changed, discount, bounds, notice, buffer, policy, tier);
    }

    ListingTerms withDiscount(DiscountRule changed) {
        return new ListingTerms(providerId, offerings, rate, travelFee, changed, bounds, notice, buffer, policy, tier);
    }

    ListingTerms withPricing(HourlyRate changedRate, TravelFeePolicy changedTravelFee, DiscountRule changedDiscount) {
        return new ListingTerms(providerId, offerings, changedRate, changedTravelFee, changedDiscount, bounds, notice,
                buffer, policy, tier);
    }

    ListingTerms withBounds(BookingBounds changed) {
        return new ListingTerms(providerId, offerings, rate, travelFee, discount, changed, notice, buffer, policy, tier);
    }

    ListingTerms withNotice(MinimumNotice changed) {
        return new ListingTerms(providerId, offerings, rate, travelFee, discount, bounds, changed, buffer, policy, tier);
    }

    ListingTerms withBuffer(JobBuffer changed) {
        return new ListingTerms(providerId, offerings, rate, travelFee, discount, bounds, notice, changed, policy, tier);
    }

    ListingTerms withPolicy(AcceptancePolicy changed, MinimumNotice noticeUnderIt) {
        return new ListingTerms(providerId, offerings, rate, travelFee, discount, bounds, noticeUnderIt, buffer, changed, tier);
    }

    ListingTerms withTier(CancellationTier changed) {
        return new ListingTerms(providerId, offerings, rate, travelFee, discount, bounds, notice, buffer, policy, changed);
    }
}
