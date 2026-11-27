package com.homa.listing.usecase;

import static com.homa.listing.usecase.ListingTestBed.KEMAS_HOME_ID;
import static com.homa.listing.usecase.ListingTestBed.kemasHomeBookability;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.listing.domain.ProviderStanding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRV-2")
class HandleProviderBookabilityChangedIETest {

    private final ListingTestBed bed = new ListingTestBed().withKemasHomeLive();

    @Test
    @DisplayName("When all its helpers are deactivated, Kemas Home stops being bookable and leaves search")
    void aProviderThatStoppedBeingBookableIsHidden() {
        bed.bookabilityChanged.handle(kemasHomeBookability("provider-event-1", false, "NO_ACTIVE_HELPERS"));

        ProviderStanding standing = bed.standings.findById(KEMAS_HOME_ID).orElseThrow();
        assertThat(standing.live()).isTrue();
        assertThat(standing.bookable()).isFalse();
        assertThat(standing.isListed()).isFalse();
        assertThat(bed.processed.handled("provider-event-1", "listing.ProviderBookabilityChanged")).isTrue();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("When a helper is back, Kemas Home is bookable and shows again")
    void aProviderBookableAgainShowsAgain() {
        bed.bookabilityChanged.handle(kemasHomeBookability("provider-event-1", false, "NO_ACTIVE_HELPERS"));

        bed.bookabilityChanged.handle(kemasHomeBookability("provider-event-2", true, null));

        assertThat(bed.standings.findById(KEMAS_HOME_ID).orElseThrow().isListed()).isTrue();
    }

    @Test
    @DisplayName("Bookability heard before going live is kept, but the provider shows only once live")
    void bookabilityBeforeGoingLiveIsKept() {
        ListingTestBed notLive = new ListingTestBed();

        notLive.bookabilityChanged.handle(kemasHomeBookability("provider-event-1", true, null));

        ProviderStanding standing = notLive.standings.findById(KEMAS_HOME_ID).orElseThrow();
        assertThat(standing.bookable()).isTrue();
        assertThat(standing.isListed()).isFalse();
    }

    @Test
    @DisplayName("A redelivered ProviderBookabilityChangedIE changes nothing and publishes nothing")
    void aRedeliveryChangesNothing() {
        bed.bookabilityChanged.handle(kemasHomeBookability("provider-event-1", false, "NO_ACTIVE_HELPERS"));
        bed.bookabilityChanged.handle(kemasHomeBookability("provider-event-2", true, null));

        bed.bookabilityChanged.handle(kemasHomeBookability("provider-event-1", false, "NO_ACTIVE_HELPERS"));

        assertThat(bed.standings.findById(KEMAS_HOME_ID).orElseThrow().isListed()).isTrue();
        assertThat(bed.publisher.published()).isEmpty();
    }
}
