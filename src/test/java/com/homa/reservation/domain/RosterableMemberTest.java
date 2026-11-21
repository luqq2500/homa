package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.reservation.domain.ReservationFixtures.deactivated;
import static com.homa.reservation.domain.ReservationFixtures.rina;
import static com.homa.reservation.domain.ReservationFixtures.siti;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.fixtures.HomaFixtures;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class RosterableMemberTest {

    @Test
    @Tag("ROS-3")
    @DisplayName("Siti goes on leave and is deactivated, so she can't be rostered until she is reactivated.")
    void onlyAnActiveMemberCanWorkOnAJob() {
        assertThat(siti().canWorkOn(JOB_END)).isTrue();
        assertThat(deactivated(siti()).canWorkOn(JOB_END)).isFalse();
    }

    @Test
    @Tag("ROS-4")
    @DisplayName("Rina's permit expires on 20 March, so she can be rostered on jobs ending up to 20 December, counted on the Kuala Lumpur calendar date. Kemas Home can't roster her on a job on 2 January.")
    void foreignMemberWorksOnlyOnJobsEndingByTheDateProviderGives() {
        assertThat(rina().rosterableUntil()).contains(LocalDate.of(2026, 12, 20));
        assertThat(rina().canWorkOn(HomaFixtures.at(2026, 12, 20, 23, 59))).isTrue();
        assertThat(rina().canWorkOn(HomaFixtures.at(2026, 12, 21, 0, 0))).isFalse();
        assertThat(rina().canWorkOn(HomaFixtures.at(2027, 1, 2, 14, 0))).isFalse();
        assertThat(deactivated(rina()).canWorkOn(JOB_END)).isFalse();
    }

    @Test
    @Tag("ROS-4")
    @DisplayName("Siti is a citizen and needs no permit, so any job date is fine for her.")
    void citizenHasNoLastDate() {
        assertThat(siti().rosterableUntil()).isEmpty();
        assertThat(siti().canWorkOn(HomaFixtures.at(2099, 12, 31, 23, 0))).isTrue();
    }

    @Test
    @DisplayName("A member belongs to its provider and is shown on the roster by name and photo.")
    void memberBelongsToItsProviderAndAppearsOnTheRosterByNameAndPhoto() {
        assertThat(siti().worksFor(KEMAS_HOME_PROVIDER_ID)).isTrue();
        assertThat(siti().worksFor("provider-another")).isFalse();
        assertThat(siti().asRosterMember().name()).isEqualTo("Siti");
        assertThat(siti().asRosterMember().photoReference()).isEqualTo("photo-siti");
        assertThat(siti().asRosterMember().isHelper()).isTrue();
    }
}
