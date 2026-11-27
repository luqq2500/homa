package com.homa.listing.usecase;

import static com.homa.listing.usecase.ListingTestBed.AZMAN;
import static com.homa.listing.usecase.ListingTestBed.HANA;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME_ID;
import static com.homa.listing.usecase.ListingTestBed.kemasHomeBookability;
import static com.homa.listing.usecase.ListingTestBed.kemasHomeWentLive;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.listing.domain.ProviderStanding;
import com.homa.listing.usecase.dto.ProviderCreateListingRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleProviderWentLiveIETest {

    private final ListingTestBed bed = new ListingTestBed();

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home went live: it is live and bookable, and Azman is recorded as its owner")
    void aProviderThatWentLiveIsListedAndItsOwnerRecorded() {
        bed.wentLive.handle(kemasHomeWentLive("provider-event-1"));

        ProviderStanding standing = bed.standings.findById(KEMAS_HOME_ID).orElseThrow();
        assertThat(standing.live()).isTrue();
        assertThat(standing.bookable()).isTrue();
        assertThat(standing.isListed()).isTrue();
        assertThat(bed.owners.ownerOf(KEMAS_HOME_ID)).contains(AZMAN);
        assertThat(bed.processed.handled("provider-event-1", "listing.ProviderWentLive")).isTrue();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("The owner Provider names replaces the person who created the listing")
    void theNamedOwnerReplacesTheCreator() {
        bed.withHomaPresets();
        bed.createListing.create(new ProviderCreateListingRequest(HANA, ListingTestBed.KEMAS_HOME,
                ListingTestBed.KEMAS_HOME_JOBS, List.of(), List.of(ListingTestBed.SALAK_TINGGI),
                com.homa.kernel.Money.myr(25), "REQUEST", "STANDARD"));

        bed.wentLive.handle(kemasHomeWentLive("provider-event-1"));

        assertThat(bed.owners.ownerOf(KEMAS_HOME_ID)).contains(AZMAN);
    }

    @Test
    @DisplayName("Helpers counted before going live are kept")
    void helpersCountedBeforeAreKept() {
        bed.withKemasHomeTeam();

        bed.wentLive.handle(kemasHomeWentLive("provider-event-1"));

        assertThat(bed.standings.findById(KEMAS_HOME_ID).orElseThrow().activeHelperCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("A redelivered ProviderWentLiveIE changes nothing and publishes nothing")
    void aRedeliveryChangesNothing() {
        bed.wentLive.handle(kemasHomeWentLive("provider-event-1"));
        bed.bookabilityChanged.handle(kemasHomeBookability("provider-event-2", false, "SUSPENDED"));
        bed.owners.record(KEMAS_HOME_ID, "person-after");

        bed.wentLive.handle(kemasHomeWentLive("provider-event-1"));

        ProviderStanding standing = bed.standings.findById(KEMAS_HOME_ID).orElseThrow();
        assertThat(standing.bookable()).isFalse();
        assertThat(standing.isListed()).isFalse();
        assertThat(bed.owners.ownerOf(KEMAS_HOME_ID)).contains("person-after");
        assertThat(bed.publisher.published()).isEmpty();
    }
}
