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
    @DisplayName("Rina's permit expires on 20 March, so she can work jobs up to 20 December, not 2 January.")
    void foreignMemberIsRosterableUntilNinetyDaysBeforeHerPermitExpires() {
        TeamMember rina = rina(liveKemasHome());

        assertThat(RINA_PERMIT_EXPIRY).isEqualTo(LocalDate.of(2027, 3, 20));
        assertThat(rina.rosterableUntil()).contains(LocalDate.of(2026, 12, 20));
        assertThat(rina.rosterableUntil().orElseThrow()).isBefore(LocalDate.of(2027, 1, 2));
    }

    @Test
    @Tag("ROS-4")
    @DisplayName("Siti is local, so no permit limits the jobs she can be rostered on.")
    void localMemberHasNoRosterableLimit() {
        assertThat(siti(liveKemasHome()).rosterableUntil()).isEmpty();
    }
}
