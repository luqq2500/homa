package com.homa.listing.domain;

import static com.homa.fixtures.HomaFixtures.FLAT_TRAVEL_FEE;
import static com.homa.fixtures.HomaFixtures.HOURLY_RATE_PER_HELPER;
import static com.homa.listing.domain.ListingFixtures.BATHROOM_CLEANING;
import static com.homa.listing.domain.ListingFixtures.CYBERJAYA;
import static com.homa.listing.domain.ListingFixtures.HEPA_VACUUM;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME_DISCOUNT;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME_POLICY;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME_RATE;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME_TIER;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME_TRAVEL_FEE;
import static com.homa.listing.domain.ListingFixtures.KITCHEN_CLEANING;
import static com.homa.listing.domain.ListingFixtures.LAUNDRY_FOLDING;
import static com.homa.listing.domain.ListingFixtures.PUTRAJAYA;
import static com.homa.listing.domain.ListingFixtures.SALAK_TINGGI;
import static com.homa.listing.domain.ListingFixtures.SHAH_ALAM;
import static com.homa.listing.domain.ListingFixtures.STEAM_MOP;
import static com.homa.listing.domain.ListingFixtures.homaPresets;
import static com.homa.listing.domain.ListingFixtures.kemasHomeCities;
import static com.homa.listing.domain.ListingFixtures.kemasHomeListing;
import static com.homa.listing.domain.ListingFixtures.kemasHomeOfferings;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.listing.domain.exception.NotInPresetsException;
import com.homa.listing.domain.exception.UnpricedCityException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderListingTest {

    private static final TravelFeePolicy SALAK_TINGGI_AND_PUTRAJAYA = TravelFeePolicy.byCity(Map.of(
            SALAK_TINGGI, Money.myr(10), PUTRAJAYA, Money.myr(25)));

    @Test
    @Tag("OFR-1")
    @DisplayName("Kemas Home picks \"Kitchen cleaning\", \"Laundry folding\" and \"Bathroom cleaning\", the tool \"HEPA vacuum\", "
            + "and the cities Salak Tinggi, Cyberjaya and Putrajaya.")
    void aProviderChoosesItsJobsToolsAndCitiesFromHomaPresets() {
        ProviderListing kemasHome = kemasHomeListing();

        assertThat(kemasHome.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(kemasHome.terms().jobs()).containsExactlyInAnyOrder(KITCHEN_CLEANING, LAUNDRY_FOLDING, BATHROOM_CLEANING);
        assertThat(kemasHome.terms().offerings().tools()).containsExactly(HEPA_VACUUM);
        assertThat(kemasHome.terms().cities()).containsExactlyInAnyOrderElementsOf(kemasHomeCities());
    }

    @Test
    @Tag("OFR-1")
    @DisplayName("A city, job or tool that isn't one of Homa's presets can't be chosen, on creating or on changing a listing")
    void aChoiceOutsideThePresetsIsRefused() {
        ProviderListing kemasHome = kemasHomeListing();
        Preset withoutShahAlam = Preset.of(homaPresets().jobTypes(), homaPresets().tools(), kemasHomeCities());
        Offerings addingShahAlam = new Offerings(Set.of(KITCHEN_CLEANING), Set.of(HEPA_VACUUM),
                Set.of(SALAK_TINGGI, SHAH_ALAM));

        assertThatThrownBy(() -> kemasHome.chooseOfferings(addingShahAlam, withoutShahAlam))
                .isInstanceOf(NotInPresetsException.class).hasMessageContaining("SHAH_ALAM");
        assertThat(kemasHome.terms().offerings()).isEqualTo(kemasHomeOfferings());
        assertThatThrownBy(() -> ProviderListing.create(KEMAS_HOME, addingShahAlam, withoutShahAlam, KEMAS_HOME_RATE,
                KEMAS_HOME_POLICY, KEMAS_HOME_TIER)).isInstanceOf(NotInPresetsException.class);
    }

    @Test
    @Tag("OFR-1")
    @DisplayName("Kemas Home adds Shah Alam and a steam mop once Homa offers them")
    void aProviderChangesItsOfferingsWithinThePresets() {
        ProviderListing kemasHome = kemasHomeListing();
        Offerings wider = new Offerings(Set.of(KITCHEN_CLEANING), Set.of(HEPA_VACUUM, STEAM_MOP),
                Set.of(SALAK_TINGGI, CYBERJAYA, PUTRAJAYA, SHAH_ALAM));

        ListingTermsChange change = kemasHome.chooseOfferings(wider, homaPresets());

        assertThat(change.terms().offerings()).isEqualTo(wider);
        assertThat(change.noticeRaised()).isFalse();
        assertThat(kemasHome.terms().cities()).contains(SHAH_ALAM);
    }

    @Test
    @Tag("OFR-2")
    @DisplayName("Kemas Home raises its rate from RM25 to RM30 per helper per hour")
    void aProviderChangesItsHourlyRate() {
        ProviderListing kemasHome = kemasHomeListing();

        ListingTermsChange change = kemasHome.setRate(new HourlyRate(Money.myr(30)));

        assertThat(change.terms().rate().perHelperPerHour()).isEqualTo(Money.myr(30));
        assertThat(kemasHome.terms().rate().priceFor(Duration.ofHours(2), 2)).isEqualTo(Money.myr(120));
    }

    @Test
    @Tag("OFR-3")
    @DisplayName("A provider sets RM10 for Salak Tinggi and RM25 for Putrajaya; that's refused while it also covers Cyberjaya")
    void aByCityTravelFeeMustPriceEveryCoveredCity() {
        ProviderListing kemasHome = kemasHomeListing();

        assertThatThrownBy(() -> kemasHome.setTravelFee(SALAK_TINGGI_AND_PUTRAJAYA))
                .isInstanceOf(UnpricedCityException.class).hasMessageContaining("CYBERJAYA");
        assertThat(kemasHome.terms().travelFee()).isEqualTo(KEMAS_HOME_TRAVEL_FEE);

        kemasHome.chooseOfferings(new Offerings(Set.of(KITCHEN_CLEANING), Set.of(), Set.of(SALAK_TINGGI, PUTRAJAYA)),
                homaPresets());
        ListingTermsChange change = kemasHome.setTravelFee(SALAK_TINGGI_AND_PUTRAJAYA);

        assertThat(change.terms().travelFee().feeFor(PUTRAJAYA)).isEqualTo(Money.myr(25));
    }

    @Test
    @Tag("OFR-3")
    @DisplayName("With a by-city travel fee, a provider can't add a city it hasn't priced")
    void aCityCantBeAddedWithoutATravelFeePrice() {
        ProviderListing listing = kemasHomeListing();
        listing.chooseOfferings(new Offerings(Set.of(KITCHEN_CLEANING), Set.of(), Set.of(SALAK_TINGGI, PUTRAJAYA)),
                homaPresets());
        listing.setTravelFee(SALAK_TINGGI_AND_PUTRAJAYA);

        assertThatThrownBy(() -> listing.chooseOfferings(kemasHomeOfferings(), homaPresets()))
                .isInstanceOf(UnpricedCityException.class);
    }

    @Test
    @Tag("OFR-4")
    @DisplayName("Kemas Home sets 10% for at least 3 hours and 2 helpers, and can later remove it")
    void aProviderSetsAndRemovesItsDiscount() {
        ProviderListing kemasHome = kemasHomeListing();

        assertThat(kemasHome.terms().discount()).isEqualTo(KEMAS_HOME_DISCOUNT);
        assertThat(kemasHome.setDiscount(DiscountRule.none()).terms().discount()).isEqualTo(DiscountRule.none());
    }

    @Test
    @Tag("OFR-5")
    @DisplayName("Kemas Home sets 2 to 6 hours and leaves helpers on default")
    void aProviderSetsItsBookingBounds() {
        ProviderListing kemasHome = kemasHomeListing();
        BookingBounds twoToSix = BookingBounds.defaults().withHours(Duration.ofHours(2), Duration.ofHours(6));

        ListingTermsChange change = kemasHome.setBookingBounds(twoToSix);

        assertThat(change.terms().bounds()).isEqualTo(twoToSix);
        assertThat(kemasHome.terms().bounds().maxHelpers()).isEmpty();
    }

    @Test
    @Tag("OFR-9")
    @DisplayName("Kemas Home chooses Request and Standard. These are shown on its page before a customer reserves.")
    void aProviderChoosesExactlyOneAcceptancePolicyAndOneCancellationTier() {
        ProviderListing kemasHome = kemasHomeListing();

        assertThat(kemasHome.terms().policy()).isEqualTo(AcceptancePolicy.REQUEST);
        assertThat(kemasHome.terms().tier()).isEqualTo(CancellationTier.STANDARD);

        ListingTermsChange change = kemasHome.chooseCancellationTier(CancellationTier.FIRM);

        assertThat(change.terms().tier()).isEqualTo(CancellationTier.FIRM);
        assertThat(kemasHome.terms().tier()).isEqualTo(CancellationTier.FIRM);
        assertThatThrownBy(() -> kemasHome.chooseCancellationTier(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> kemasHome.choosePolicy(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ProviderListing.create(KEMAS_HOME, kemasHomeOfferings(), homaPresets(), KEMAS_HOME_RATE,
                null, KEMAS_HOME_TIER)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @Tag("OFR-3")
    @Tag("OFR-4")
    @Tag("OFR-5")
    @Tag("OFR-6")
    @Tag("OFR-10")
    @DisplayName("A new listing starts with no travel fee, no discount, 1 to 8 hours, 2 hours' notice and a 30-minute buffer")
    void aNewListingStartsAtTheDefaults() {
        ListingTerms terms = ProviderListing.create(KEMAS_HOME, kemasHomeOfferings(), homaPresets(), KEMAS_HOME_RATE,
                KEMAS_HOME_POLICY, KEMAS_HOME_TIER).terms();

        assertThat(terms.travelFee()).isEqualTo(TravelFeePolicy.none());
        assertThat(terms.discount()).isEqualTo(DiscountRule.none());
        assertThat(terms.bounds()).isEqualTo(BookingBounds.defaults());
        assertThat(terms.notice()).isEqualTo(MinimumNotice.DEFAULT);
        assertThat(terms.buffer()).isEqualTo(JobBuffer.DEFAULT);
    }

    @Test
    @Tag("RES-5")
    @Tag("RES-8")
    @DisplayName("The terms snapshot carries Kemas Home's 2-to-6-hour bounds, so Reservation checks Aisyah's hours "
            + "against them: 2h and 2h 15m are allowed, 1h 45m and 2h 10m are not")
    void theTermsSnapshotCarriesTheBoundsReservationChecksHoursAgainst() {
        BookingBounds twoToSix = BookingBounds.defaults().withHours(Duration.ofHours(2), Duration.ofHours(6));

        ListingTerms terms = kemasHomeListing().setBookingBounds(twoToSix).terms();

        assertThat(terms.bounds()).isEqualTo(twoToSix);
        assertThat(terms.bounds().allowsHours(Duration.ofHours(2))).isTrue();
        assertThat(terms.bounds().allowsHours(Duration.ofMinutes(135))).isTrue();
        assertThat(terms.bounds().allowsHours(Duration.ofMinutes(105))).isFalse();
        assertThat(terms.bounds().allowsHours(Duration.ofMinutes(130))).isFalse();
    }

    @Test
    @Tag("RES-8")
    @DisplayName("Every change gives the full terms snapshot that Reservation freezes: Kemas Home's §0 terms")
    void everyChangeGivesTheFullTermsSnapshot() {
        ListingTerms terms = kemasHomeListing().setJobBuffer(JobBuffer.DEFAULT).terms();

        assertThat(terms.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(terms.cities()).containsExactlyInAnyOrderElementsOf(kemasHomeCities());
        assertThat(terms.jobs()).hasSize(3);
        assertThat(terms.rate().perHelperPerHour()).isEqualTo(HOURLY_RATE_PER_HELPER);
        assertThat(terms.travelFee().feeFor(CYBERJAYA)).isEqualTo(FLAT_TRAVEL_FEE);
        assertThat(terms.discount()).isEqualTo(KEMAS_HOME_DISCOUNT);
        assertThat(terms.bounds()).isEqualTo(BookingBounds.defaults());
        assertThat(terms.notice()).isEqualTo(MinimumNotice.DEFAULT);
        assertThat(terms.buffer()).isEqualTo(JobBuffer.DEFAULT);
        assertThat(terms.policy()).isEqualTo(KEMAS_HOME_POLICY);
        assertThat(terms.tier()).isEqualTo(KEMAS_HOME_TIER);
    }

    @Test
    @Tag("RES-8")
    @DisplayName("A stored listing is rebuilt from its terms snapshot unchanged")
    void aStoredListingIsRebuiltFromItsTerms() {
        ListingTerms stored = kemasHomeListing().terms();

        ProviderListing rebuilt = ProviderListing.rebuild(stored);

        assertThat(rebuilt.terms()).isEqualTo(stored);
        assertThat(rebuilt.providerId()).isEqualTo(KEMAS_HOME);
        assertThatThrownBy(() -> ProviderListing.rebuild(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @Tag("OFR-1")
    @DisplayName("A provider id can't be blank")
    void aProviderIdCantBeBlank() {
        assertThatThrownBy(() -> new ProviderId(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProviderId(null)).isInstanceOf(NullPointerException.class);
    }
}
