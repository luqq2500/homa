package com.homa.reservation.usecase.ie;

import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.TAPPED_PAY_AT;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.ProviderWentLiveIE;
import com.homa.reservation.domain.replica.ProviderReplica;
import com.homa.reservation.usecase.ReservationTestBed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleProviderWentLiveIETest {

    private final ReservationTestBed bed = new ReservationTestBed();

    private ProviderWentLiveIE wentLive(String eventId) {
        return new ProviderWentLiveIE(eventId, KEMAS_HOME_PROVIDER_ID, "person-azman", "202301012345", TAPPED_PAY_AT);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home goes live, and is bookable once Listing publishes its listing, whichever comes first.")
    void providerThatWentLiveIsBookable() {
        bed.providerWentLive.handle(wentLive("live-1"));

        assertThat(bed.providers.findById(KEMAS_HOME_PROVIDER_ID)).contains(
                ProviderReplica.unheardOf(KEMAS_HOME_PROVIDER_ID).withBookability(true));

        bed.listingTermsChanged.handle(bed.kemasListing().build("listing-1"));
        assertThat(bed.providers.findById(KEMAS_HOME_PROVIDER_ID).orElseThrow().bookable()).isTrue();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A redelivered go-live doesn't undo a later loss of bookability.")
    void redeliveredGoLiveIsIgnored() {
        ProviderWentLiveIE live = wentLive("live-1");
        bed.providerWentLive.handle(live);
        bed.bookabilityChanged.handle(bed.bookability(false, "NO_VERIFIED_PAYOUT_ACCOUNT"));

        bed.providerWentLive.handle(live);

        assertThat(bed.providers.findById(KEMAS_HOME_PROVIDER_ID).orElseThrow().bookable()).isFalse();
        assertThat(HandleProviderWentLiveIE.HANDLER).isEqualTo("reservation.ProviderWentLive");
        assertThat(bed.publisher.published()).isEmpty();
    }
}
