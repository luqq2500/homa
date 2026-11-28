package com.homa.reservation.usecase.ie;

import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.AZMAN_PERSON_ID;
import static com.homa.reservation.usecase.ReservationTestBed.TAPPED_PAY_AT;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.ProviderOpenedIE;
import com.homa.reservation.domain.replica.ProviderReplica;
import com.homa.reservation.usecase.ReservationTestBed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleProviderOpenedIETest {

    private final ReservationTestBed bed = new ReservationTestBed();

    private ProviderOpenedIE opened(String eventId, String ownerPersonId) {
        return new ProviderOpenedIE(eventId, KEMAS_HOME_PROVIDER_ID, ownerPersonId, TAPPED_PAY_AT);
    }

    @Test
    @Tag("PRV-11")
    @DisplayName("Azman, verified, opens Kemas Home: Reservation keeps him as its owner, who acts on all its reservations.")
    void ownerIsKeptFromTheOpenedProvider() {
        bed.providerOpened.handle(opened("opened-1", AZMAN_PERSON_ID));

        assertThat(bed.providers.findById(KEMAS_HOME_PROVIDER_ID)).contains(
                ProviderReplica.unheardOf(KEMAS_HOME_PROVIDER_ID).ownedBy(AZMAN_PERSON_ID));
        assertThat(bed.providers.count()).isEqualTo(1);
    }

    @Test
    @Tag("PRV-11")
    @DisplayName("Kemas Home went live and was listed before Reservation heard who opened it: hearing it changes only the owner.")
    void ownerChangesNothingElseOfTheProvider() {
        bed.providerWentLive.handle(new com.homa.ie.events.ProviderWentLiveIE("live-1", KEMAS_HOME_PROVIDER_ID,
                AZMAN_PERSON_ID, "202301012345", TAPPED_PAY_AT));
        bed.listingTermsChanged.handle(bed.kemasListing().build("listing-1"));
        ProviderReplica before = bed.providers.findById(KEMAS_HOME_PROVIDER_ID).orElseThrow();

        bed.providerOpened.handle(opened("opened-1", AZMAN_PERSON_ID));

        assertThat(bed.providers.findById(KEMAS_HOME_PROVIDER_ID)).contains(before.ownedBy(AZMAN_PERSON_ID));
        assertThat(bed.providers.findById(KEMAS_HOME_PROVIDER_ID).orElseThrow().bookable()).isTrue();
        assertThat(bed.providers.findById(KEMAS_HOME_PROVIDER_ID).orElseThrow().listing()).isEqualTo(before.listing());
    }

    @Test
    @Tag("PRV-11")
    @DisplayName("A redelivered opening changes nothing and publishes nothing.")
    void redeliveredOpeningIsIgnored() {
        ProviderOpenedIE opening = opened("opened-1", AZMAN_PERSON_ID);
        bed.providerOpened.handle(opening);
        bed.providers.save(bed.providers.findById(KEMAS_HOME_PROVIDER_ID).orElseThrow().ownedBy("person-someone-else"));

        bed.providerOpened.handle(opening);

        assertThat(bed.providers.findById(KEMAS_HOME_PROVIDER_ID).orElseThrow().ownerPersonId())
                .contains("person-someone-else");
        assertThat(HandleProviderOpenedIE.HANDLER).isEqualTo("reservation.ProviderOpened");
        assertThat(bed.publisher.published()).isEmpty();
    }
}
