package com.homa.listing.usecase;

import static com.homa.listing.usecase.ListingTestBed.AZMAN;
import static com.homa.listing.usecase.ListingTestBed.CYBERJAYA;
import static com.homa.listing.usecase.ListingTestBed.HANA;
import static com.homa.listing.usecase.ListingTestBed.HANA_CLEANING;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME;
import static com.homa.listing.usecase.ListingTestBed.KITCHEN_CLEANING;
import static com.homa.listing.usecase.ListingTestBed.PUTRAJAYA;
import static com.homa.listing.usecase.ListingTestBed.SALAK_TINGGI;
import static com.homa.listing.usecase.ListingTestBed.SHAH_ALAM;
import static com.homa.listing.usecase.ListingTestBed.kemasHomeBookability;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.ProviderWentLiveIE;
import com.homa.kernel.Money;
import com.homa.listing.usecase.dto.ProviderCreateListingRequest;
import com.homa.listing.usecase.dto.ProviderSetPricingTermsRequest;
import com.homa.listing.usecase.dto.SearchProvidersByCityRequest;
import com.homa.listing.usecase.dto.SearchProvidersByCityResponse;
import com.homa.listing.usecase.dto.SearchProvidersByCityResponse.FoundProvider;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("RES-1")
class SearchProvidersByCityTest {

    private final ListingTestBed bed = new ListingTestBed().withKemasHomeTeam().withKemasHomeListing();

    @Test
    @DisplayName("Aisyah in Salak Tinggi finds Kemas Home and Hana Cleaning, each with its rate and travel fee to her city")
    void aCustomerFindsTheLiveBookableProvidersCoveringHerCity() {
        bed.setPricingTerms.set(ProviderSetPricingTermsTest.kemasHomePricing(AZMAN));
        bed.withKemasHomeLive();
        hanaCleaningLive(Map.of(SALAK_TINGGI, Money.myr(10)));

        SearchProvidersByCityResponse response = bed.search.search(new SearchProvidersByCityRequest(SALAK_TINGGI));

        assertThat(response).isEqualTo(new SearchProvidersByCityResponse(SALAK_TINGGI, List.of(
                new FoundProvider(HANA_CLEANING, Money.myr(30), Money.myr(10)),
                new FoundProvider(KEMAS_HOME, Money.myr(25), Money.myr(20)))));
    }

    @Test
    @DisplayName("Her sister in Shah Alam finds neither: they don't cover Shah Alam")
    void aProviderNotCoveringTheCityIsNotFound() {
        bed.withKemasHomeLive();
        hanaCleaningLive(Map.of(SALAK_TINGGI, Money.myr(10)));

        assertThat(bed.search.search(new SearchProvidersByCityRequest(SHAH_ALAM)).providers()).isEmpty();
        assertThat(bed.search.search(new SearchProvidersByCityRequest(PUTRAJAYA)).providers())
                .extracting(FoundProvider::providerId).containsExactly(KEMAS_HOME);
    }

    @Test
    @DisplayName("Kemas Home's listing doesn't show before it goes live")
    void aProviderThatIsNotLiveIsNotFound() {
        assertThat(bed.search.search(new SearchProvidersByCityRequest(SALAK_TINGGI)).providers()).isEmpty();
    }

    @Test
    @DisplayName("When all its helpers are deactivated for a holiday week, Kemas Home disappears from search")
    void aProviderThatIsNotBookableIsNotFound() {
        bed.withKemasHomeLive();
        bed.bookabilityChanged.handle(kemasHomeBookability("provider-event-off", false, "NO_ACTIVE_HELPERS"));

        assertThat(bed.search.search(new SearchProvidersByCityRequest(CYBERJAYA)).providers()).isEmpty();
    }

    @Test
    @DisplayName("A provider with no travel fee shows RM0 to the city")
    void noTravelFeeShowsZero() {
        bed.withKemasHomeLive();

        assertThat(bed.search.search(new SearchProvidersByCityRequest(CYBERJAYA)).providers())
                .containsExactly(new FoundProvider(KEMAS_HOME, Money.myr(25), Money.myr(0)));
    }

    private void hanaCleaningLive(Map<String, Money> travelFeeByCity) {
        bed.createListing.create(new ProviderCreateListingRequest(HANA, HANA_CLEANING, List.of(KITCHEN_CLEANING),
                List.of(), List.of(SALAK_TINGGI), Money.myr(30), "INSTANT", "FLEXIBLE"));
        bed.setPricingTerms.set(new ProviderSetPricingTermsRequest(HANA, HANA_CLEANING, Money.myr(30), "BY_CITY", null,
                travelFeeByCity, null, 0, 0));
        bed.wentLive.handle(new ProviderWentLiveIE("provider-event-hana-live", HANA_CLEANING, HANA, null,
                ListingTestBed.NOW));
    }
}
