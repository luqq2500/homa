package com.homa.reservation.domain;

import static com.homa.reservation.domain.ReservationFixtures.AZMAN_PERSON_ID;
import static com.homa.reservation.domain.ReservationFixtures.FARID_PERSON_ID;
import static com.homa.reservation.domain.ReservationFixtures.NURUL_PERSON_ID;
import static com.homa.reservation.domain.ReservationFixtures.RINA_PERSON_ID;
import static com.homa.reservation.domain.ReservationFixtures.SITI_PERSON_ID;
import static com.homa.reservation.domain.ReservationFixtures.STRANGER_PERSON_ID;
import static com.homa.reservation.domain.ReservationFixtures.farid;
import static com.homa.reservation.domain.ReservationFixtures.friday;
import static com.homa.reservation.domain.ReservationFixtures.nurul;
import static com.homa.reservation.domain.ReservationFixtures.rina;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.siti;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** PRV-11: who may act on a reservation for its provider is a domain rule; security only maps the token to a person. */
class ReservationActorsTest {

    private final Reservation reserved = Marketplace.withKemasHome().reserveStandard();

    @Test
    @Tag("PRV-11")
    @DisplayName("Azman, Kemas Home's owner, acts on its reservation before anyone is rostered, and Siti, who is on the team, can't yet.")
    void theOwnerActsOnEveryReservationOfTheirProviderAndTeamMembersNotYetRostered() {
        assertThat(reserved.ownerPersonId()).isEqualTo(AZMAN_PERSON_ID);
        assertThat(reserved.canBeActedOnBy(AZMAN_PERSON_ID)).isTrue();
        assertThat(reserved.canBeActedOnBy(SITI_PERSON_ID)).isFalse();
        assertThat(reserved.canBeActedOnBy(STRANGER_PERSON_ID)).isFalse();
    }

    @Test
    @Tag("PRV-11")
    @DisplayName("Siti, rostered on Saturday, acts for Kemas Home on it with her own sign-in; Rina, not rostered, can't.")
    void aRosteredMemberActsOnlyOnTheReservationsTheyAreRosteredOn() {
        reserved.assignRoster(List.of(siti(), nurul(), farid()), friday(18, 0));

        assertThat(reserved.canBeActedOnBy(SITI_PERSON_ID)).isTrue();
        assertThat(reserved.canBeActedOnBy(NURUL_PERSON_ID)).isTrue();
        assertThat(reserved.canBeActedOnBy(FARID_PERSON_ID)).as("a supervisor is rostered too").isTrue();
        assertThat(reserved.canBeActedOnBy(RINA_PERSON_ID)).isFalse();
        assertThat(reserved.canBeActedOnBy(STRANGER_PERSON_ID)).isFalse();
    }

    @Test
    @Tag("PRV-11")
    @DisplayName("Azman acts on Saturday's reservation whoever is rostered on it, himself not among them.")
    void theOwnerKeepsActingOnceARosterIsSet() {
        reserved.assignRoster(List.of(siti(), nurul()), friday(18, 0));

        assertThat(reserved.canBeActedOnBy(AZMAN_PERSON_ID)).isTrue();
    }

    @Test
    @Tag("PRV-11")
    @DisplayName("Kemas Home swaps Nurul for Rina at 8:00 on Saturday: Rina now acts for it on that reservation, and Nurul no longer does.")
    void aMemberReplacedOnTheRosterStopsActingAndTheirReplacementStarts() {
        reserved.assignRoster(List.of(siti(), nurul()), friday(18, 0));

        reserved.assignRoster(List.of(siti(), rina()), saturday(8, 0));

        assertThat(reserved.canBeActedOnBy(RINA_PERSON_ID)).isTrue();
        assertThat(reserved.canBeActedOnBy(NURUL_PERSON_ID)).isFalse();
        assertThat(reserved.canBeActedOnBy(SITI_PERSON_ID)).isTrue();
    }

    @Test
    @Tag("PRV-11")
    @DisplayName("Another provider's owner, and Aisyah, are not Kemas Home's owner and so can't act for it.")
    void aPersonWhoIsNotTheOwnerOrRosteredCannotAct() {
        reserved.assignRoster(List.of(siti(), nurul()), friday(18, 0));

        assertThat(reserved.canBeActedOnBy("person-hana")).isFalse();
        assertThat(reserved.canBeActedOnBy("customer-aisyah")).isFalse();
    }
}
