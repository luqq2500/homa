package com.homa.listing.domain;

import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HELPERS;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HOURS;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME_DISCOUNT;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME_POLICY;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME_RATE;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME_TIER;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME_TRAVEL_FEE;
import static com.homa.listing.domain.ListingFixtures.homaPresets;
import static com.homa.listing.domain.ListingFixtures.instantListing;
import static com.homa.listing.domain.ListingFixtures.kemasHomeListing;
import static com.homa.listing.domain.ListingFixtures.kemasHomeOfferings;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import com.homa.listing.domain.exception.BelowMinimumRateException;
import com.homa.listing.domain.exception.ListingRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("OFR-11")
class ProviderListingMinimumRateTest {

    private static final HourlyRate FORTY = new HourlyRate(Money.myr(40));

    private static DiscountRule discountOf(long rate) {
        return DiscountRule.of(Percentage.of(rate), DISCOUNT_MIN_HOURS, DISCOUNT_MIN_HELPERS);
    }

    @Test
    @DisplayName("At RM25, Kemas Home's largest discount is 20%: it can raise its discount to 20% but not to 21%")
    void aBiggerDiscountThatBreaksTheMinimumRateIsRefused() {
        ProviderListing kemasHome = kemasHomeListing();

        assertThatThrownBy(() -> kemasHome.setDiscount(discountOf(21)))
                .isInstanceOf(BelowMinimumRateException.class)
                .hasMessageContaining("MYR 19.75")
                .extracting(e -> ((ListingRuleViolationException) e).ruleId()).isEqualTo("OFR-11");
        assertThat(kemasHome.terms().discount()).isEqualTo(KEMAS_HOME_DISCOUNT);

        assertThat(kemasHome.setDiscount(discountOf(20)).terms().discount()).isEqualTo(discountOf(20));
    }

    @Test
    @DisplayName("With its 10% discount, Kemas Home can't lower its rate to RM22, which would leave RM19.80")
    void loweringTheRateBelowWhatTheDiscountAllowsIsRefused() {
        ProviderListing kemasHome = kemasHomeListing();

        assertThatThrownBy(() -> kemasHome.setRate(new HourlyRate(Money.myr(22))))
                .isInstanceOf(BelowMinimumRateException.class);
        assertThat(kemasHome.terms().rate()).isEqualTo(KEMAS_HOME_RATE);

        HourlyRate lowestWithTenPercent = new HourlyRate(Money.myr("22.23"));
        assertThat(kemasHome.setRate(lowestWithTenPercent).terms().rate()).isEqualTo(lowestWithTenPercent);
    }

    @Test
    @DisplayName("Without a discount, a rate of RM19.99 is refused, on creating or on changing a listing; RM20 is allowed")
    void aRateBelowTwentyRinggitWithNoDiscountIsRefused() {
        ProviderListing listing = instantListing();
        HourlyRate underMinimum = new HourlyRate(Money.myr("19.99"));
        HourlyRate minimum = new HourlyRate(Money.myr(20));

        assertThatThrownBy(() -> listing.setRate(underMinimum)).isInstanceOf(BelowMinimumRateException.class);
        assertThat(listing.terms().rate()).isEqualTo(KEMAS_HOME_RATE);
        assertThat(listing.setRate(minimum).terms().rate()).isEqualTo(minimum);
        assertThatThrownBy(() -> ProviderListing.create(KEMAS_HOME, kemasHomeOfferings(), homaPresets(), underMinimum,
                KEMAS_HOME_POLICY, KEMAS_HOME_TIER)).isInstanceOf(BelowMinimumRateException.class);
        assertThat(ProviderListing.create(KEMAS_HOME, kemasHomeOfferings(), homaPresets(), minimum, KEMAS_HOME_POLICY,
                KEMAS_HOME_TIER).terms().rate()).isEqualTo(minimum);
    }

    @Test
    @DisplayName("Kemas Home moves from RM25 with 10% to RM40 with 50% in one change; RM40 with 51% is refused and "
            + "nothing changes")
    void aRateAndDiscountSetTogetherAreCheckedTogether() {
        ProviderListing kemasHome = kemasHomeListing();
        TravelFeePolicy noTravelFee = TravelFeePolicy.none();

        assertThatThrownBy(() -> kemasHome.setPricingTerms(FORTY, noTravelFee, discountOf(51)))
                .isInstanceOf(BelowMinimumRateException.class);
        assertThat(kemasHome.terms().rate()).isEqualTo(KEMAS_HOME_RATE);
        assertThat(kemasHome.terms().travelFee()).isEqualTo(KEMAS_HOME_TRAVEL_FEE);
        assertThat(kemasHome.terms().discount()).isEqualTo(KEMAS_HOME_DISCOUNT);

        ListingTermsChange change = kemasHome.setPricingTerms(FORTY, noTravelFee, discountOf(50));

        assertThat(change.noticeRaised()).isFalse();
        assertThat(change.terms().rate()).isEqualTo(FORTY);
        assertThat(change.terms().travelFee()).isEqualTo(noTravelFee);
        assertThat(change.terms().discount()).isEqualTo(discountOf(50));
        assertThat(kemasHome.terms()).isEqualTo(change.terms());
    }

    @Test
    @Tag("OFR-4")
    @DisplayName("A 99% discount is within the maximum but must still respect the minimum rate: refused at RM25, "
            + "allowed at RM2,000")
    void theMaximumDiscountStillRespectsTheMinimumRate() {
        ProviderListing kemasHome = kemasHomeListing();
        HourlyRate twoThousand = new HourlyRate(Money.myr(2000));

        assertThatThrownBy(() -> kemasHome.setDiscount(discountOf(99))).isInstanceOf(BelowMinimumRateException.class);
        assertThat(kemasHome.setPricingTerms(twoThousand, KEMAS_HOME_TRAVEL_FEE, discountOf(99)).terms().discount())
                .isEqualTo(discountOf(99));
    }

    @Test
    @DisplayName("Removing the discount never breaks the minimum rate")
    void removingTheDiscountIsAlwaysAllowed() {
        ProviderListing kemasHome = kemasHomeListing();

        assertThat(kemasHome.setDiscount(DiscountRule.none()).terms().discount()).isEqualTo(DiscountRule.none());
    }

    @Test
    @DisplayName("A listing stored before a change to Homa's minimum rate still loads with its rate")
    void aStoredListingIsNotRecheckedAgainstTheMinimumRate() {
        ListingTerms storedAtNineteen = kemasHomeListing().terms().withRate(new HourlyRate(Money.myr(19)));

        assertThat(ProviderListing.rebuild(storedAtNineteen).terms()).isEqualTo(storedAtNineteen);
    }
}
