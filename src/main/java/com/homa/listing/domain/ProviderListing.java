package com.homa.listing.domain;

import com.homa.listing.domain.exception.NotInPresetsException;
import java.util.List;
import java.util.Objects;

/**
 * A provider's listing on Homa: the offerings and settings of §3.4. The provider creates it by choosing what has no
 * default (jobs and cities, the rate, the acceptance policy and the cancellation tier); everything else starts at
 * its default (no travel fee, no discount, 1 to 8 hours, 1 helper up to the active team, 2 hours' notice, 30
 * minutes' buffer). Every change returns the full terms snapshot so it can be published (RES-8).
 */
public final class ProviderListing {

    private ListingTerms terms;

    private ProviderListing(ListingTerms terms) {
        this.terms = Objects.requireNonNull(terms, "terms");
    }

    /** A new listing, with the provider's offerings chosen from Homa's presets (OFR-1) and defaults elsewhere. */
    public static ProviderListing create(ProviderId providerId, Offerings offerings, Preset preset, HourlyRate rate,
                                         AcceptancePolicy policy, CancellationTier tier) {
        requireFromPresets(offerings, preset);
        return new ProviderListing(new ListingTerms(providerId, offerings, rate, TravelFeePolicy.none(),
                DiscountRule.none(), BookingBounds.defaults(), MinimumNotice.DEFAULT, JobBuffer.DEFAULT, policy, tier));
    }

    /** A listing as it was stored. Presets aren't re-checked: a choice made from them stays valid. */
    public static ProviderListing rebuild(ListingTerms terms) {
        return new ProviderListing(terms);
    }

    public ProviderId providerId() {
        return terms.providerId();
    }

    public ListingTerms terms() {
        return terms;
    }

    /** OFR-1: jobs, tools and cities covered, only from presets. A by-city travel fee must price each city (OFR-3). */
    public ListingTermsChange chooseOfferings(Offerings offerings, Preset preset) {
        requireFromPresets(offerings, preset);
        return changeTo(terms.withOfferings(offerings));
    }

    /** OFR-2. */
    public ListingTermsChange setRate(HourlyRate rate) {
        return changeTo(terms.withRate(rate));
    }

    /** OFR-3. */
    public ListingTermsChange setTravelFee(TravelFeePolicy travelFee) {
        return changeTo(terms.withTravelFee(travelFee));
    }

    /** OFR-4; {@link DiscountRule#none()} removes the discount. */
    public ListingTermsChange setDiscount(DiscountRule discount) {
        return changeTo(terms.withDiscount(discount));
    }

    /** OFR-5. */
    public ListingTermsChange setBookingBounds(BookingBounds bounds) {
        return changeTo(terms.withBounds(bounds));
    }

    /** OFR-6: refused below the current policy's floor. */
    public ListingTermsChange setMinimumNotice(MinimumNotice notice) {
        return changeTo(terms.withNotice(notice));
    }

    /** OFR-10. */
    public ListingTermsChange setJobBuffer(JobBuffer buffer) {
        return changeTo(terms.withBuffer(buffer));
    }

    /**
     * OFR-9: the one acceptance policy, replacing the previous one. Switching to Request with a notice under 2 hours
     * raises the notice to 2 hours, and the change says so, so the provider is told (OFR-6).
     */
    public ListingTermsChange choosePolicy(AcceptancePolicy policy) {
        MinimumNotice notice = terms.notice().raisedToFloorOf(policy);
        boolean noticeRaised = !notice.equals(terms.notice());
        terms = terms.withPolicy(policy, notice);
        return new ListingTermsChange(terms, noticeRaised);
    }

    /** OFR-9: the one customer cancellation tier, replacing the previous one. */
    public ListingTermsChange chooseCancellationTier(CancellationTier tier) {
        return changeTo(terms.withTier(tier));
    }

    private ListingTermsChange changeTo(ListingTerms changed) {
        terms = changed;
        return new ListingTermsChange(changed, false);
    }

    private static void requireFromPresets(Offerings offerings, Preset preset) {
        List<String> notInPresets = preset.notInPresets(offerings);
        if (!notInPresets.isEmpty()) {
            throw new NotInPresetsException("Not one of Homa's presets: " + notInPresets);
        }
    }
}
