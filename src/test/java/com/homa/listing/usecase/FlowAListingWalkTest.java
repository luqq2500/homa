package com.homa.listing.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CITY;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HELPERS;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HOURS;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_RATE;
import static com.homa.fixtures.HomaFixtures.FLAT_TRAVEL_FEE;
import static com.homa.fixtures.HomaFixtures.HOURLY_RATE_PER_HELPER;
import static com.homa.fixtures.HomaFixtures.JOB_BUFFER;
import static com.homa.fixtures.HomaFixtures.MINIMUM_NOTICE;
import static com.homa.listing.usecase.ListingTestBed.ADMIN;
import static com.homa.listing.usecase.ListingTestBed.AZMAN;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME;
import static com.homa.listing.usecase.ListingTestBed.kemasHomeCreation;
import static com.homa.listing.usecase.ListingTestBed.kemasHomeWentLive;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.AvailabilityChangedIE;
import com.homa.ie.events.ListingTermsChangedIE;
import com.homa.listing.usecase.dto.AdminManagePresetsRequest;
import com.homa.listing.usecase.dto.ProviderSetAvailabilityRequest;
import com.homa.listing.usecase.dto.SearchProvidersByCityRequest;
import com.homa.listing.usecase.dto.SearchProvidersByCityResponse;
import com.homa.listing.usecase.dto.SearchProvidersByCityResponse.FoundProvider;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Flow A, step 1, as far as Listing goes: Kemas Home's listing is set up as §0 has it, Kemas Home goes live, and
 * Aisyah's search for Salak Tinggi finds it with its RM25 rate and RM20 travel fee. Reservation quotes the RM200
 * from the terms Listing published.
 */
class FlowAListingWalkTest {

    private final ListingTestBed bed = new ListingTestBed();

    @Test
    @Tag("RES-1")
    @Tag("OFR-1")
    @Tag("OFR-11")
    @DisplayName("Kemas Home's listing is created with RM25, RM20 flat and 10% from 3 hours and 2 helpers; it goes "
            + "live; Aisyah's search for Salak Tinggi finds it")
    void aisyahFindsKemasHome() {
        bed.managePresets.manage(new AdminManagePresetsRequest(ADMIN, "ADMIN",
                List.of("KITCHEN_CLEANING", "LAUNDRY_FOLDING", "BATHROOM_CLEANING"), List.of("HEPA_VACUUM"),
                List.of("SALAK_TINGGI", "CYBERJAYA", "PUTRAJAYA", "SHAH_ALAM")));
        bed.withKemasHomeTeam();
        bed.opened.handle(ListingTestBed.kemasHomeOpened("provider-event-opened"));

        bed.createListing.create(kemasHomeCreation());
        bed.setPricingTerms.set(ProviderSetPricingTermsTest.kemasHomePricing(AZMAN));
        bed.setAvailability.set(new ProviderSetAvailabilityRequest(AZMAN, KEMAS_HOME, List.of(
                "MONDAY 08:00-18:00", "TUESDAY 08:00-18:00", "WEDNESDAY 08:00-18:00", "THURSDAY 08:00-18:00",
                "FRIDAY 08:00-18:00", "SATURDAY 08:00-18:00")));

        assertThat(bed.search.search(new SearchProvidersByCityRequest(AISYAH_CITY)).providers())
                .as("not live yet").isEmpty();

        bed.wentLive.handle(kemasHomeWentLive("provider-event-live"));

        SearchProvidersByCityResponse found = bed.search.search(new SearchProvidersByCityRequest(AISYAH_CITY));
        assertThat(found.providers())
                .containsExactly(new FoundProvider(KEMAS_HOME, HOURLY_RATE_PER_HELPER, FLAT_TRAVEL_FEE));
        assertThat(bed.search.search(new SearchProvidersByCityRequest("SHAH_ALAM")).providers())
                .as("her sister in Shah Alam").isEmpty();

        List<ListingTermsChangedIE> terms = bed.publisher.published().stream()
                .filter(ListingTermsChangedIE.class::isInstance).map(ListingTermsChangedIE.class::cast).toList();
        ListingTermsChangedIE latest = terms.get(terms.size() - 1);
        assertThat(terms).hasSize(2);
        assertThat(latest.hourlyRatePerHelper()).isEqualTo(HOURLY_RATE_PER_HELPER);
        assertThat(latest.travelFeeKind()).isEqualTo("FLAT");
        assertThat(latest.flatTravelFee()).isEqualTo(FLAT_TRAVEL_FEE);
        assertThat(latest.discountRate()).isEqualTo(DISCOUNT_RATE);
        assertThat(latest.discountMinHours()).isEqualTo(DISCOUNT_MIN_HOURS);
        assertThat(latest.discountMinHelpers()).isEqualTo(DISCOUNT_MIN_HELPERS);
        assertThat(latest.maxHelpers()).isEqualTo(3);
        assertThat(latest.minimumNotice()).isEqualTo(MINIMUM_NOTICE);
        assertThat(latest.jobBuffer()).isEqualTo(JOB_BUFFER);
        assertThat(latest.acceptancePolicy()).isEqualTo("REQUEST");
        assertThat(latest.cancellationTier()).isEqualTo("STANDARD");
        assertThat(bed.publisher.published()).filteredOn(AvailabilityChangedIE.class::isInstance).hasSize(1);
    }
}
