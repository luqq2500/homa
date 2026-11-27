package com.homa.listing.usecase;

import static com.homa.listing.usecase.ListingTestBed.AZMAN;
import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME_ID;
import static com.homa.listing.usecase.ListingTestBed.kemasHomeCreation;
import static com.homa.listing.usecase.ListingTestBed.kemasHomeOpened;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleProviderOpenedIETest {

    private final ListingTestBed bed = new ListingTestBed().withHomaPresets();

    @Test
    @Tag("PRV-1")
    @DisplayName("Azman opened Kemas Home: he is recorded as its owner, and can then create its listing")
    void theOpeningOwnerIsRecorded() {
        bed.opened.handle(kemasHomeOpened("provider-event-1"));

        assertThat(bed.owners.ownerOf(KEMAS_HOME_ID)).contains(AZMAN);
        assertThat(bed.processed.handled("provider-event-1", "listing.ProviderOpened")).isTrue();
        assertThat(bed.publisher.published()).isEmpty();
        bed.createListing.create(kemasHomeCreation());
        assertThat(bed.listings.termsOf(KEMAS_HOME_ID)).isPresent();
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("A redelivered ProviderOpenedIE changes nothing and publishes nothing")
    void aRedeliveryChangesNothing() {
        bed.opened.handle(kemasHomeOpened("provider-event-1"));
        bed.owners.record(KEMAS_HOME_ID, "person-after");

        bed.opened.handle(kemasHomeOpened("provider-event-1"));

        assertThat(bed.owners.ownerOf(KEMAS_HOME_ID)).contains("person-after");
        assertThat(bed.publisher.published()).isEmpty();
    }
}
