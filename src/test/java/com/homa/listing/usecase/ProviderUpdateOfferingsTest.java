package com.homa.listing.usecase;

import static com.homa.listing.usecase.ListingTestBed.AZMAN;
import static com.homa.listing.usecase.ListingTestBed.CYBERJAYA;
import static com.homa.listing.usecase.ListingTestBed.HANA;
import static com.homa.listing.usecase.ListingTestBed.HANA_CLEANING;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME_ID;
import static com.homa.listing.usecase.ListingTestBed.KITCHEN_CLEANING;
import static com.homa.listing.usecase.ListingTestBed.SALAK_TINGGI;
import static com.homa.listing.usecase.ListingTestBed.SHAH_ALAM;
import static com.homa.listing.usecase.ListingTestBed.STEAM_MOP;
import static com.homa.listing.usecase.ListingTestBed.WINDOW_CLEANING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ListingTermsChangedIE;
import com.homa.listing.domain.City;
import com.homa.listing.domain.JobType;
import com.homa.listing.domain.ListingTerms;
import com.homa.listing.domain.ProviderId;
import com.homa.listing.domain.Tool;
import com.homa.listing.domain.exception.NotInPresetsException;
import com.homa.listing.usecase.dto.ProviderUpdateOfferingsRequest;
import com.homa.listing.usecase.exception.ListingNotFoundException;
import com.homa.listing.usecase.exception.NotAllowedException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("OFR-1")
class ProviderUpdateOfferingsTest {

    private final ListingTestBed bed = new ListingTestBed().withKemasHomeListing();

    @Test
    @DisplayName("Kemas Home adds window cleaning and a steam mop and covers Shah Alam instead of Putrajaya; the full "
            + "terms are published")
    void aProviderChangesItsOfferingsFromThePresets() {
        ProviderUpdateOfferingsRequest request = new ProviderUpdateOfferingsRequest(AZMAN, KEMAS_HOME,
                List.of(KITCHEN_CLEANING, WINDOW_CLEANING), List.of(STEAM_MOP), List.of(SALAK_TINGGI, CYBERJAYA, SHAH_ALAM));

        assertThat(bed.updateOfferings.update(request).providerId()).isEqualTo(KEMAS_HOME);

        ListingTerms stored = bed.listings.termsOf(KEMAS_HOME_ID).orElseThrow();
        assertThat(stored.jobs()).extracting(JobType::code).containsExactlyInAnyOrder(KITCHEN_CLEANING, WINDOW_CLEANING);
        assertThat(stored.offerings().tools()).extracting(Tool::code).containsExactly(STEAM_MOP);
        assertThat(stored.cities()).extracting(City::code).containsExactlyInAnyOrder(SALAK_TINGGI, CYBERJAYA, SHAH_ALAM);
        ListingTermsChangedIE event = bed.publisher.single(ListingTermsChangedIE.class);
        assertThat(event).isEqualTo(new ListingTermsChangedIE("listing-event-2", KEMAS_HOME,
                List.of(CYBERJAYA, SALAK_TINGGI, SHAH_ALAM), List.of(KITCHEN_CLEANING, WINDOW_CLEANING),
                stored.rate().perHelperPerHour(), "NONE", null, java.util.Map.of(), null, null, 0,
                java.time.Duration.ofHours(1), java.time.Duration.ofHours(8), java.time.Duration.ofMinutes(15), 1, 0,
                java.time.Duration.ofHours(2), java.time.Duration.ofMinutes(30), "REQUEST", "STANDARD", false,
                ListingTestBed.NOW));
    }

    @Test
    @DisplayName("A job that isn't a preset is refused; the listing stays as it was and nothing is published")
    void aChoiceOutsideThePresetsChangesNothing() {
        ProviderUpdateOfferingsRequest request = new ProviderUpdateOfferingsRequest(AZMAN, KEMAS_HOME,
                List.of("POOL_CLEANING"), List.of(), List.of(SALAK_TINGGI));

        assertThatThrownBy(() -> bed.updateOfferings.update(request)).isInstanceOf(NotInPresetsException.class);

        assertThat(bed.listings.termsOf(KEMAS_HOME_ID).orElseThrow().jobs()).hasSize(3);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("Only Kemas Home's owner changes its listing: Hana is refused")
    void onlyTheOwnerChangesTheListing() {
        ProviderUpdateOfferingsRequest byHana = new ProviderUpdateOfferingsRequest(HANA, KEMAS_HOME,
                List.of(KITCHEN_CLEANING), List.of(), List.of(SALAK_TINGGI));

        assertThatThrownBy(() -> bed.updateOfferings.update(byHana))
                .isInstanceOf(NotAllowedException.class)
                .hasMessageContaining(HANA).hasMessageContaining(KEMAS_HOME);

        assertThat(bed.listings.termsOf(KEMAS_HOME_ID).orElseThrow().jobs()).hasSize(3);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("A live provider without a listing has nothing to change yet")
    void aProviderWithoutAListingIsRefused() {
        bed.owners.record(new ProviderId(HANA_CLEANING), HANA);
        ProviderUpdateOfferingsRequest request = new ProviderUpdateOfferingsRequest(HANA, HANA_CLEANING,
                List.of(KITCHEN_CLEANING), List.of(), List.of(SALAK_TINGGI));

        assertThatThrownBy(() -> bed.updateOfferings.update(request))
                .isInstanceOf(ListingNotFoundException.class)
                .hasMessageContaining(HANA_CLEANING);
        assertThat(bed.publisher.published()).isEmpty();
    }
}
