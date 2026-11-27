package com.homa.customer.domain;

import static com.homa.customer.domain.CustomerFixtures.AISYAH_ARRIVAL_NOTE;
import static com.homa.customer.domain.CustomerFixtures.AISYAH_STREET_ADDRESS;
import static com.homa.customer.domain.CustomerFixtures.aisyah;
import static com.homa.customer.domain.CustomerFixtures.aisyahsGoogleAccount;
import static com.homa.customer.domain.CustomerFixtures.aisyahsMyKad;
import static com.homa.customer.domain.CustomerRegistrationTest.assertMissing;
import static com.homa.fixtures.HomaFixtures.AISYAH_CITY;
import static com.homa.fixtures.HomaFixtures.AISYAH_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerProfileTest {

    @Test
    @Tag("CUS-1")
    @DisplayName("Aisyah moves from Salak Tinggi to Cyberjaya. She enters her new address, with its city, and a new "
            + "arrival note, and changes how her name is written.")
    void customerChangesHerHomeAddressArrivalNoteAndName() {
        Customer aisyah = aisyah();
        HomeAddress cyberjaya = new HomeAddress("A-3-1, Residensi Cyber, Persiaran Multimedia",
                new CityCode("CYBERJAYA"));

        boolean moved = aisyah.changeHomeAddress(cyberjaya);
        aisyah.changeArrivalNote(new ArrivalNote("Lobby: ask the concierge for unit A-3-1"));
        aisyah.changeName(new CustomerName("Aisyah R."));

        assertThat(moved).isTrue();
        assertThat(aisyah.homeAddress()).isEqualTo(cyberjaya);
        assertThat(aisyah.homeAddress().city().value()).isEqualTo("CYBERJAYA");
        assertThat(aisyah.arrivalNote().value()).isEqualTo("Lobby: ask the concierge for unit A-3-1");
        assertThat(aisyah.name().value()).isEqualTo("Aisyah R.");
        assertThat(aisyah.signInId()).isEqualTo(aisyahsGoogleAccount());
        assertThat(aisyah.verifiedIdentity()).contains(aisyahsMyKad());
    }

    @Test
    @Tag("CUS-1")
    @DisplayName("Aisyah can't clear her name, home address or arrival note; her account keeps the ones she had.")
    void customerKeepsEveryDetailWhenAChangeLeavesOneOut() {
        Customer aisyah = aisyah();

        assertMissing(() -> aisyah.changeName(null), "a name");
        assertMissing(() -> aisyah.changeHomeAddress(null), "a home address");
        assertMissing(() -> aisyah.changeArrivalNote(null), "an arrival note");

        assertThat(aisyah.name().value()).isEqualTo(AISYAH_NAME);
        assertThat(aisyah.homeAddress().streetAddress()).isEqualTo(AISYAH_STREET_ADDRESS);
        assertThat(aisyah.homeAddress().city().value()).isEqualTo(AISYAH_CITY);
        assertThat(aisyah.arrivalNote().value()).isEqualTo(AISYAH_ARRIVAL_NOTE);
    }

    @Test
    @Tag("CUS-1")
    @DisplayName("Aisyah saves her profile with the Salak Tinggi address she already has, or with only a new unit "
            + "number on the same street; only the second is a move.")
    void customerMovesHomeOnlyWhenTheAddressDiffers() {
        Customer aisyah = aisyah();

        boolean sameAddressMoved = aisyah.changeHomeAddress(new HomeAddress(AISYAH_STREET_ADDRESS,
                new CityCode(AISYAH_CITY)));
        boolean newUnitMoved = aisyah.changeHomeAddress(new HomeAddress("B-7-1, Residensi Salak, Jalan Salak 1",
                new CityCode(AISYAH_CITY)));

        assertThat(sameAddressMoved).isFalse();
        assertThat(newUnitMoved).isTrue();
        assertThat(aisyah.homeAddress().streetAddress()).isEqualTo("B-7-1, Residensi Salak, Jalan Salak 1");
    }

    @Test
    @Tag("CUS-1")
    @Tag("RES-8")
    @DisplayName("Aisyah saves her profile with the arrival note she already has, then with a new one: only the new "
            + "note is a change.")
    void arrivalNoteChangesOnlyWhenItDiffers() {
        Customer aisyah = aisyah();

        boolean sameNoteChanged = aisyah.changeArrivalNote(new ArrivalNote(AISYAH_ARRIVAL_NOTE));
        boolean newNoteChanged = aisyah.changeArrivalNote(new ArrivalNote("Guardhouse: say unit B-12-3, then lift B"));

        assertThat(sameNoteChanged).isFalse();
        assertThat(newNoteChanged).isTrue();
        assertThat(aisyah.arrivalNote().value()).isEqualTo("Guardhouse: say unit B-12-3, then lift B");
    }
}
