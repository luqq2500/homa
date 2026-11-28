package com.homa.provider.domain;

import static com.homa.provider.domain.ProviderFixtures.AZMAN;
import static com.homa.provider.domain.ProviderFixtures.HANA;
import static com.homa.provider.domain.ProviderFixtures.HANA_CLEANING;
import static com.homa.provider.domain.ProviderFixtures.KEMAS_HOME;
import static com.homa.provider.domain.ProviderFixtures.NURUL;
import static com.homa.provider.domain.ProviderFixtures.SITI;
import static com.homa.provider.domain.ProviderFixtures.SITI_PERSON;
import static com.homa.provider.domain.ProviderFixtures.TEAM_ADDED_AT;
import static com.homa.provider.domain.ProviderFixtures.azman;
import static com.homa.provider.domain.ProviderFixtures.hana;
import static com.homa.provider.domain.ProviderFixtures.nurulVerified;
import static com.homa.provider.domain.ProviderFixtures.openedHanaCleaning;
import static com.homa.provider.domain.ProviderFixtures.openedKemasHome;
import static com.homa.provider.domain.ProviderFixtures.sitiVerified;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.provider.domain.exception.PersonAlreadyBelongsToProviderException;
import com.homa.provider.domain.exception.ProviderRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class AffiliationTest {

    private static final Affiliation SITI_ON_KEMAS_HOME_TEAM = Affiliation.of(null, KEMAS_HOME);
    private static final Affiliation AZMAN_OWNS_KEMAS_HOME = Affiliation.of(KEMAS_HOME, null);

    @Test
    @Tag("HLP-10")
    @DisplayName("Someone who belongs to no provider can open one and join a team.")
    void personWhoBelongsToNobodyCanOpenAndJoin() {
        Provider hanaCleaning = Provider.open(HANA_CLEANING, hana(), Affiliation.none());
        TeamMember siti = hanaCleaning.addTeamMember(
                SITI, TeamRole.HELPER, sitiVerified(), false, Affiliation.none(), TEAM_ADDED_AT);

        assertThat(hanaCleaning.isRunBy(HANA)).isTrue();
        assertThat(siti.providerId()).isEqualTo(HANA_CLEANING);
        assertThat(Affiliation.none().ownedProvider()).isEmpty();
        assertThat(Affiliation.none().teamProvider()).isEmpty();
    }

    @Test
    @Tag("HLP-10")
    @DisplayName("Siti, a Kemas Home helper, can't open her own provider until Kemas Home removes her.")
    void memberCantOpenAProvider() {
        assertThatThrownBy(() -> Provider.open(new ProviderId("provider-siti"), sitiVerified(), SITI_ON_KEMAS_HOME_TEAM))
                .isInstanceOf(PersonAlreadyBelongsToProviderException.class)
                .isInstanceOf(ProviderRuleViolationException.class)
                .hasMessageContaining(SITI_PERSON.value())
                .hasMessageContaining("HLP-10");
    }

    @Test
    @Tag("HLP-10")
    @DisplayName("Azman, who owns Kemas Home, can't open a second provider.")
    void ownerCantOpenASecondProvider() {
        assertThatThrownBy(() -> Provider.open(new ProviderId("provider-azman-2"), azman(), AZMAN_OWNS_KEMAS_HOME))
                .isInstanceOf(PersonAlreadyBelongsToProviderException.class)
                .hasMessageContaining(AZMAN.value());
    }

    @Test
    @Tag("HLP-10")
    @DisplayName("Siti, a Kemas Home helper, can't join Hana Cleaning until Kemas Home removes her.")
    void memberCantJoinAnotherProvider() {
        Provider hanaCleaning = openedHanaCleaning();

        assertThatThrownBy(() -> hanaCleaning.addTeamMember(
                SITI, TeamRole.HELPER, sitiVerified(), false, SITI_ON_KEMAS_HOME_TEAM, TEAM_ADDED_AT))
                .isInstanceOf(PersonAlreadyBelongsToProviderException.class)
                .hasMessageContaining(SITI_PERSON.value())
                .hasMessageNotContaining(KEMAS_HOME.value());
    }

    @Test
    @Tag("HLP-10")
    @DisplayName("Azman, who owns Kemas Home, can't join Hana Cleaning's team.")
    void ownerOfAnotherProviderCantJoinATeam() {
        Provider hanaCleaning = openedHanaCleaning();

        assertThatThrownBy(() -> hanaCleaning.addTeamMember(
                new TeamMemberId("member-azman"), TeamRole.SUPERVISOR, azman(), false, AZMAN_OWNS_KEMAS_HOME,
                TEAM_ADDED_AT))
                .isInstanceOf(PersonAlreadyBelongsToProviderException.class);
    }

    @Test
    @Tag("HLP-10")
    @DisplayName("Nurul is already on Kemas Home's team, so she can't be added to it a second time.")
    void memberCantBeAddedToTheSameTeamTwice() {
        Provider kemasHome = openedKemasHome();

        assertThatThrownBy(() -> kemasHome.addTeamMember(
                NURUL, TeamRole.HELPER, nurulVerified(), false, SITI_ON_KEMAS_HOME_TEAM, TEAM_ADDED_AT))
                .isInstanceOf(PersonAlreadyBelongsToProviderException.class);
    }

    @Test
    @Tag("HLP-10")
    @DisplayName("Data that puts a person on two providers is refused whichever one they try to join.")
    void personOnTwoProvidersIsRefusedEverywhere() {
        Affiliation brokenData = Affiliation.of(HANA_CLEANING, KEMAS_HOME);

        assertThatThrownBy(() -> Provider.open(new ProviderId("provider-x"), hana(), brokenData))
                .isInstanceOf(PersonAlreadyBelongsToProviderException.class);
        assertThatThrownBy(() -> openedKemasHome().addTeamMember(
                new TeamMemberId("member-x"), TeamRole.HELPER, hana(), false, brokenData, TEAM_ADDED_AT))
                .isInstanceOf(PersonAlreadyBelongsToProviderException.class);
        assertThatThrownBy(() -> openedHanaCleaning().addTeamMember(
                new TeamMemberId("member-y"), TeamRole.HELPER, hana(), false, brokenData, TEAM_ADDED_AT))
                .isInstanceOf(PersonAlreadyBelongsToProviderException.class);
    }

    @Test
    @Tag("HLP-10")
    @DisplayName("An affiliation says which provider a person owns and which team they are on.")
    void affiliationNamesTheProvidersTheyBelongTo() {
        Affiliation both = Affiliation.of(HANA_CLEANING, HANA_CLEANING);

        assertThat(both.ownedProvider()).contains(HANA_CLEANING);
        assertThat(both.teamProvider()).contains(HANA_CLEANING);
        assertThat(SITI_ON_KEMAS_HOME_TEAM.ownedProvider()).isEmpty();
        assertThat(SITI_ON_KEMAS_HOME_TEAM.teamProvider()).contains(KEMAS_HOME);
        assertThat(AZMAN_OWNS_KEMAS_HOME.ownedProvider()).contains(KEMAS_HOME);
        assertThat(AZMAN_OWNS_KEMAS_HOME.teamProvider()).isEmpty();
    }
}
