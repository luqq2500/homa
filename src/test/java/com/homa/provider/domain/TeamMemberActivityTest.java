package com.homa.provider.domain;

import static com.homa.provider.domain.ProviderFixtures.FARID;
import static com.homa.provider.domain.ProviderFixtures.KEMAS_HOME;
import static com.homa.provider.domain.ProviderFixtures.NURUL;
import static com.homa.provider.domain.ProviderFixtures.RINA;
import static com.homa.provider.domain.ProviderFixtures.RINA_PERSON;
import static com.homa.provider.domain.ProviderFixtures.SITI;
import static com.homa.provider.domain.ProviderFixtures.TEAM_ADDED_AT;
import static com.homa.provider.domain.ProviderFixtures.VERIFIED_UNTIL;
import static com.homa.provider.domain.ProviderFixtures.farid;
import static com.homa.provider.domain.ProviderFixtures.openedKemasHome;
import static com.homa.provider.domain.ProviderFixtures.rinaPermit;
import static com.homa.provider.domain.ProviderFixtures.storedMember;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class TeamMemberActivityTest {

    @Test
    @Tag("PRV-2")
    @DisplayName("A helper is active when verified, not suspended and not deactivated by the provider.")
    void helperIsActiveOnlyWhenNeitherDeactivatedNorSuspended() {
        TeamMember active = storedMember(SITI, TeamRole.HELPER, false, false);
        TeamMember deactivated = storedMember(SITI, TeamRole.HELPER, true, false);
        TeamMember suspended = storedMember(SITI, TeamRole.HELPER, false, true);
        TeamMember both = storedMember(SITI, TeamRole.HELPER, true, true);

        assertThat(active.isActive()).isTrue();
        assertThat(active.isActiveHelper()).isTrue();
        assertThat(deactivated.isActive()).isFalse();
        assertThat(deactivated.isActiveHelper()).isFalse();
        assertThat(suspended.isActive()).isFalse();
        assertThat(suspended.isActiveHelper()).isFalse();
        assertThat(both.isActive()).isFalse();
    }

    @Test
    @Tag("PRV-3")
    @DisplayName("Farid is an active supervisor, but not an active helper.")
    void activeSupervisorIsNotAnActiveHelper() {
        TeamMember farid = farid(openedKemasHome());

        assertThat(farid.isActive()).isTrue();
        assertThat(farid.isActiveHelper()).isFalse();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home's team counts only its active helpers: Siti counts; deactivated Nurul, suspended Rina and"
            + " supervisor Farid don't.")
    void teamCountsOnlyActiveHelpers() {
        Team team = new Team(List.of(
                storedMember(SITI, TeamRole.HELPER, false, false),
                storedMember(NURUL, TeamRole.HELPER, true, false),
                storedMember(RINA, TeamRole.HELPER, false, true),
                storedMember(FARID, TeamRole.SUPERVISOR, false, false)));

        assertThat(team.activeHelperCount()).isEqualTo(1);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A team is fixed when it is read; later changes to the list it was read from don't change it.")
    void teamKeepsTheMembersItWasReadWith() {
        List<TeamMember> members = new ArrayList<>(List.of(storedMember(SITI, TeamRole.HELPER, false, false)));
        Team team = new Team(members);

        members.add(storedMember(NURUL, TeamRole.HELPER, false, false));

        assertThat(team.members()).hasSize(1);
        assertThat(team.activeHelperCount()).isEqualTo(1);
    }

    @Test
    @Tag("HLP-3")
    @DisplayName("A stored team member is rebuilt as it was saved, with their person and permit.")
    void restoredMemberKeepsItsStoredState() {
        TeamMember rina = TeamMember.restore(RINA, KEMAS_HOME, RINA_PERSON, TeamRole.HELPER, rinaPermit(),
                TEAM_ADDED_AT, false, true, false, false, VERIFIED_UNTIL);

        assertThat(rina.id()).isEqualTo(RINA);
        assertThat(rina.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(rina.personId()).isEqualTo(RINA_PERSON);
        assertThat(rina.role()).isEqualTo(TeamRole.HELPER);
        assertThat(rina.eligibility()).isEqualTo(rinaPermit());
        assertThat(rina.addedAt()).isEqualTo(TEAM_ADDED_AT);
        assertThat(rina.hasVerifiedPhoto()).isFalse();
        assertThat(rina.isDeactivated()).isTrue();
        assertThat(rina.isSuspended()).isFalse();
        assertThat(storedMember(SITI, TeamRole.HELPER, false, true).hasVerifiedPhoto()).isTrue();
        assertThat(storedMember(SITI, TeamRole.HELPER, false, true).isSuspended()).isTrue();
        assertThat(storedMember(SITI, TeamRole.HELPER, false, true).isDeactivated()).isFalse();
    }
}
