package com.homa.customer.domain;

import static com.homa.customer.domain.CustomerFixtures.AISYAH_ARRIVAL_NOTE;
import static com.homa.customer.domain.CustomerFixtures.AISYAH_STREET_ADDRESS;
import static com.homa.customer.domain.CustomerFixtures.aisyah;
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
    @DisplayName("Aisyah moves from Salak Tinggi to Cyberjaya and enters her new address and arrival note. Her "
            + "account keeps every detail, with the new city.")
    void customerChangesHerHomeAddressArrivalNoteAndUserName() {
        Customer aisyah = aisyah();
        HomeAddress cyberjaya = new HomeAddress("A-3-1, Residensi Cyber, Persiaran Multimedia",
                new CityCode("CYBERJAYA"));

        aisyah.changeHomeAddress(cyberjaya);
        aisyah.changeArrivalNote(new ArrivalNote("Lobby: call 012-345 6789"));
        aisyah.changeUserName(new UserName("Aisyah R."));

        assertThat(aisyah.homeAddress()).isEqualTo(cyberjaya);
        assertThat(aisyah.homeAddress().city().value()).isEqualTo("CYBERJAYA");
        assertThat(aisyah.arrivalNote().value()).isEqualTo("Lobby: call 012-345 6789");
        assertThat(aisyah.userName().value()).isEqualTo("Aisyah R.");
    }

    @Test
    @Tag("CUS-1")
    @DisplayName("Aisyah can't clear her user name, home address or arrival note; her account keeps the ones she had.")
    void customerKeepsEveryDetailWhenAChangeLeavesOneOut() {
        Customer aisyah = aisyah();

        assertMissing(() -> aisyah.changeUserName(null), "a user name");
        assertMissing(() -> aisyah.changeHomeAddress(null), "a home address");
        assertMissing(() -> aisyah.changeArrivalNote(null), "an arrival note");

        assertThat(aisyah.userName().value()).isEqualTo(AISYAH_NAME);
        assertThat(aisyah.homeAddress().streetAddress()).isEqualTo(AISYAH_STREET_ADDRESS);
        assertThat(aisyah.homeAddress().city().value()).isEqualTo(AISYAH_CITY);
        assertThat(aisyah.arrivalNote().value()).isEqualTo(AISYAH_ARRIVAL_NOTE);
    }
}
