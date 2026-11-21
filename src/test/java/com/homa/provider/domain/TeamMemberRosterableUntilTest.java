package com.homa.provider.domain;

import static com.homa.provider.domain.ProviderFixtures.RINA_PERMIT_EXPIRY;
import static com.homa.provider.domain.ProviderFixtures.liveKemasHome;
import static com.homa.provider.domain.ProviderFixtures.rina;
import static com.homa.provider.domain.ProviderFixtures.siti;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class TeamMemberRosterableUntilTest {

    @Test
    @Tag("ROS-4")
    @DisplayName("Rina's permit expires on 21 May 2027, so she can be rostered only on jobs ending by 20 February 2027,"
            + " 90 days earlier.")
    void foreignMemberIsRosterableUntilNinetyDaysBeforeHerPermitExpires() {
        TeamMember rina = rina(liveKemasHome());

        assertThat(RINA_PERMIT_EXPIRY).isEqualTo(LocalDate.of(2027, 5, 21));
        assertThat(rina.rosterableUntil()).contains(LocalDate.of(2027, 2, 20));
    }

    @Test
    @Tag("ROS-4")
    @DisplayName("Siti is a citizen, so no permit limits the jobs she can be rostered on.")
    void citizenHasNoRosterableLimit() {
        assertThat(siti(liveKemasHome()).rosterableUntil()).isEmpty();
    }
}
