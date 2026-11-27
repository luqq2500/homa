package com.homa.listing.usecase;

import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HELPERS;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HOURS;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_RATE;
import static com.homa.fixtures.HomaFixtures.FLAT_TRAVEL_FEE;
import static com.homa.fixtures.HomaFixtures.HOURLY_RATE_PER_HELPER;
import static com.homa.listing.usecase.ListingTestBed.AZMAN;
import static com.homa.listing.usecase.ListingTestBed.CYBERJAYA;
import static com.homa.listing.usecase.ListingTestBed.HANA;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME_ID;
import static com.homa.listing.usecase.ListingTestBed.PUTRAJAYA;
import static com.homa.listing.usecase.ListingTestBed.SALAK_TINGGI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ListingTermsChangedIE;
import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import com.homa.listing.domain.DiscountRule;
import com.homa.listing.domain.ListingTerms;
import com.homa.listing.domain.TravelFeePolicy;
import com.homa.listing.domain.exception.BelowMinimumRateException;
import com.homa.listing.domain.exception.UnpricedCityException;
import com.homa.listing.usecase.dto.ProviderSetPricingTermsRequest;
import com.homa.listing.usecase.exception.NotAllowedException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderSetPricingTermsTest {

    private final ListingTestBed bed = new ListingTestBed().withKemasHomeListing();

    @Test
    @Tag("OFR-2")
    @Tag("OFR-3")
    @Tag("OFR-4")
    @DisplayName("Kemas Home sets RM25, RM20 flat and 10% for at least 3 hours and 2 helpers; the full terms are published")
    void kemasHomeSetsItsPricingTerms() {
        assertThat(bed.setPricingTerms.set(kemasHomePricing(AZMAN)).providerId()).isEqualTo(KEMAS_HOME);

        ListingTerms stored = bed.listings.termsOf(KEMAS_HOME_ID).orElseThrow();
        assertThat(stored.rate().perHelperPerHour()).isEqualTo(HOURLY_RATE_PER_HELPER);
        assertThat(stored.travelFee()).isEqualTo(TravelFeePolicy.flat(FLAT_TRAVEL_FEE));
        assertThat(stored.discount()).isEqualTo(DiscountRule.of(DISCOUNT_RATE, DISCOUNT_MIN_HOURS, DISCOUNT_MIN_HELPERS));
        ListingTermsChangedIE event = bed.publisher.single(ListingTermsChangedIE.class);
        assertThat(event.eventId()).isEqualTo("listing-event-2");
        assertThat(event.hourlyRatePerHelper()).isEqualTo(Money.myr(25));
        assertThat(event.travelFeeKind()).isEqualTo("FLAT");
        assertThat(event.flatTravelFee()).isEqualTo(Money.myr(20));
        assertThat(event.travelFeeByCity()).isEmpty();
        assertThat(event.discountRate()).isEqualTo(Percentage.of(10));
        assertThat(event.discountMinHours()).isEqualTo(Duration.ofHours(3));
        assertThat(event.discountMinHelpers()).isEqualTo(2);
        assertThat(event.noticeRaised()).isFalse();
    }

    @Test
    @Tag("OFR-3")
    @DisplayName("Another provider sets RM10 for Salak Tinggi and RM25 for Putrajaya, and prices Cyberjaya too")
    void aTravelFeeByCityIsPublishedPerCity() {
        Map<String, Money> byCity = Map.of(SALAK_TINGGI, Money.myr(10), PUTRAJAYA, Money.myr(25), CYBERJAYA, Money.myr(15));

        bed.setPricingTerms.set(new ProviderSetPricingTermsRequest(AZMAN, KEMAS_HOME, Money.myr(25), "BY_CITY",
                Money.myr(99), byCity, null, 0, 0));

        ListingTermsChangedIE event = bed.publisher.single(ListingTermsChangedIE.class);
        assertThat(event.travelFeeKind()).isEqualTo("BY_CITY");
        assertThat(event.flatTravelFee()).isNull();
        assertThat(event.travelFeeByCity()).isEqualTo(byCity);
        assertThat(event.discountRate()).isNull();
        assertThat(event.discountMinHours()).isNull();
        assertThat(event.discountMinHelpers()).isZero();
    }

    @Test
    @Tag("OFR-3")
    @DisplayName("A by-city fee that leaves a covered city unpriced is refused")
    void aByCityFeeMustPriceEveryCoveredCity() {
        ProviderSetPricingTermsRequest request = new ProviderSetPricingTermsRequest(AZMAN, KEMAS_HOME, Money.myr(25),
                "BY_CITY", null, Map.of(SALAK_TINGGI, Money.myr(10)), null, 0, 0);

        assertThatThrownBy(() -> bed.setPricingTerms.set(request)).isInstanceOf(UnpricedCityException.class);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("OFR-3")
    @Tag("OFR-4")
    @DisplayName("Without a travel fee or a discount, the terms say so")
    void noTravelFeeAndNoDiscount() {
        bed.setPricingTerms.set(kemasHomePricing(AZMAN));
        bed.publisher.clear();

        bed.setPricingTerms.set(new ProviderSetPricingTermsRequest(AZMAN, KEMAS_HOME, Money.myr(30), "NONE",
                Money.myr(20), Map.of(SALAK_TINGGI, Money.myr(10)), null, 180, 2));

        ListingTerms stored = bed.listings.termsOf(KEMAS_HOME_ID).orElseThrow();
        assertThat(stored.travelFee()).isEqualTo(TravelFeePolicy.none());
        assertThat(stored.discount()).isEqualTo(DiscountRule.none());
        ListingTermsChangedIE event = bed.publisher.single(ListingTermsChangedIE.class);
        assertThat(event.hourlyRatePerHelper()).isEqualTo(Money.myr(30));
        assertThat(event.travelFeeKind()).isEqualTo("NONE");
        assertThat(event.flatTravelFee()).isNull();
        assertThat(event.discountRate()).isNull();
    }

    @Test
    @Tag("OFR-11")
    @DisplayName("A provider charging RM40 can offer 50%: rate and discount are checked together")
    void rateAndDiscountAreCheckedTogether() {
        bed.setPricingTerms.set(new ProviderSetPricingTermsRequest(AZMAN, KEMAS_HOME, Money.myr(40), "NONE", null,
                Map.of(), BigDecimal.valueOf(50), 120, 1));

        ListingTermsChangedIE event = bed.publisher.single(ListingTermsChangedIE.class);
        assertThat(event.hourlyRatePerHelper()).isEqualTo(Money.myr(40));
        assertThat(event.discountRate()).isEqualTo(Percentage.of(50));
        assertThat(event.discountMinHours()).isEqualTo(Duration.ofHours(2));
        assertThat(event.discountMinHelpers()).isEqualTo(1);
    }

    @Test
    @Tag("OFR-11")
    @DisplayName("At RM25, Kemas Home's largest discount is 20%: 25% is refused and nothing changes")
    void aDiscountBelowTheMinimumRateIsRefused() {
        ProviderSetPricingTermsRequest request = new ProviderSetPricingTermsRequest(AZMAN, KEMAS_HOME, Money.myr(25),
                "FLAT", FLAT_TRAVEL_FEE, Map.of(), BigDecimal.valueOf(25), 180, 2);

        assertThatThrownBy(() -> bed.setPricingTerms.set(request)).isInstanceOf(BelowMinimumRateException.class);

        assertThat(bed.listings.termsOf(KEMAS_HOME_ID).orElseThrow().discount()).isEqualTo(DiscountRule.none());
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("An unknown travel fee kind is refused")
    void anUnknownTravelFeeKindIsRefused() {
        ProviderSetPricingTermsRequest request = new ProviderSetPricingTermsRequest(AZMAN, KEMAS_HOME, Money.myr(25),
                "PER_KM", null, Map.of(), null, 0, 0);

        assertThatThrownBy(() -> bed.setPricingTerms.set(request)).isInstanceOf(IllegalArgumentException.class);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("Only Kemas Home's owner sets its prices")
    void onlyTheOwnerSetsPrices() {
        assertThatThrownBy(() -> bed.setPricingTerms.set(kemasHomePricing(HANA))).isInstanceOf(NotAllowedException.class);

        assertThat(bed.listings.termsOf(KEMAS_HOME_ID).orElseThrow().travelFee()).isEqualTo(TravelFeePolicy.none());
        assertThat(bed.publisher.published()).isEmpty();
    }

    static ProviderSetPricingTermsRequest kemasHomePricing(String actingPersonId) {
        return new ProviderSetPricingTermsRequest(actingPersonId, KEMAS_HOME, HOURLY_RATE_PER_HELPER, "FLAT",
                FLAT_TRAVEL_FEE, Map.of(), DISCOUNT_RATE.value(), DISCOUNT_MIN_HOURS.toMinutes(), DISCOUNT_MIN_HELPERS);
    }
}
