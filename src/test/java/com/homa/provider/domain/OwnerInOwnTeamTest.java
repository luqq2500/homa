package com.homa.provider.domain;

import static com.homa.provider.domain.ProviderFixtures.AZMAN;
import static com.homa.provider.domain.ProviderFixtures.HANA;
import static com.homa.provider.domain.ProviderFixtures.HANA_CLEANING;
import static com.homa.provider.domain.ProviderFixtures.KEMAS_HOME;
import static com.homa.provider.domain.ProviderFixtures.TEAM_ADDED_AT;
import static com.homa.provider.domain.ProviderFixtures.VERIFIED_UNTIL;
import static com.homa.provider.domain.ProviderFixtures.azman;
import static com.homa.provider.domain.ProviderFixtures.hana;
import static com.homa.provider.domain.ProviderFixtures.openedHanaCleaning;
import static com.homa.provider.domain.ProviderFixtures.openedKemasHome;
import static com.homa.provider.domain.ProviderFixtures.sitiVerified;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.provider.domain.Eligibility.Local;
import com.homa.provider.domain.exception.MemberUnderageException;
import com.homa.provider.domain.exception.PersonAlreadyBelongsToProviderException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class OwnerInOwnTeamTest {

    private static final TeamMemberId HANA_MEMBER = new TeamMemberId("member-hana");
    private static final TeamMemberId AZMAN_MEMBER = new TeamMemberId("member-azman");

    /** Hana owns Hana Cleaning, so what the lookup finds for her is the provider she owns. */
    private static final Affiliation HANA_OWNS_HANA_CLEANING = Affiliation.of(HANA_CLEANING, null);

    @Test
    @Tag("PRV-6")
    @DisplayName("Hana is Hana Cleaning's only helper: its team has one active helper, and it can go live.")
    void ownerWorksAsAHelperInTheirOwnTeam() {
        Provider hanaCleaning = openedHanaCleaning();
        hanaCleaning.recordPayoutAccountVerified(PayoutAccountHolder.OWNER);

        TeamMember hanaMember = hanaCleaning.addOwnerToTeam(
                HANA_MEMBER, TeamRole.HELPER, hana(), HANA_OWNS_HANA_CLEANING, TEAM_ADDED_AT);
        Team team = new Team(List.of(hanaMember));
        hanaCleaning.goLive(team.activeHelperCount());

        assertThat(hanaMember.id()).isEqualTo(HANA_MEMBER);
        assertThat(hanaMember.providerId()).isEqualTo(HANA_CLEANING);
        assertThat(hanaMember.personId()).isEqualTo(hanaCleaning.owner().personId());
        assertThat(hanaMember.role()).isEqualTo(TeamRole.HELPER);
        assertThat(hanaMember.eligibility()).isEqualTo(new Local());
        assertThat(hanaMember.addedAt()).isEqualTo(TEAM_ADDED_AT);
        assertThat(hanaMember.verifiedUntil()).isEqualTo(VERIFIED_UNTIL);
        assertThat(hanaMember.isActiveHelper()).isTrue();
        assertThat(hanaCleaning.isLive()).isTrue();
        assertThat(hanaCleaning.bookabilityFor(team.activeHelperCount())).isEqualTo(Bookability.BOOKABLE);
    }

    @Test
    @Tag("PRV-6")
    @DisplayName("Azman can work as Kemas Home's supervisor: he is active but not a helper.")
    void ownerWorksAsASupervisorInTheirOwnTeam() {
        Provider kemasHome = openedKemasHome();

        TeamMember azmanMember = kemasHome.addOwnerToTeam(
                AZMAN_MEMBER, TeamRole.SUPERVISOR, azman(), Affiliation.of(KEMAS_HOME, null), TEAM_ADDED_AT);

        assertThat(azmanMember.role()).isEqualTo(TeamRole.SUPERVISOR);
        assertThat(azmanMember.isActive()).isTrue();
        assertThat(azmanMember.isActiveHelper()).isFalse();
        assertThat(azmanMember.personId().value()).isEqualTo(AZMAN.value());
    }

    @Test
    @Tag("PRV-6")
    @Tag("HLP-10")
    @DisplayName("The owner joins their own team once: they are on it already, so joining again is refused.")
    void ownerCantJoinTheirOwnTeamTwice() {
        Provider hanaCleaning = openedHanaCleaning();

        assertThatThrownBy(() -> hanaCleaning.addOwnerToTeam(HANA_MEMBER, TeamRole.HELPER, hana(),
                Affiliation.of(HANA_CLEANING, HANA_CLEANING), TEAM_ADDED_AT))
                .isInstanceOf(PersonAlreadyBelongsToProviderException.class);
    }

    @Test
    @Tag("PRV-6")
    @Tag("HLP-10")
    @DisplayName("An owner whose lookup says they own another provider is refused, as Hana would be at Kemas Home.")
    void ownerOfAnotherProviderCantJoinThisTeam() {
        Provider kemasHome = openedKemasHome();

        assertThatThrownBy(() -> kemasHome.addOwnerToTeam(HANA_MEMBER, TeamRole.HELPER, azman(),
                Affiliation.of(HANA_CLEANING, null), TEAM_ADDED_AT))
                .isInstanceOf(PersonAlreadyBelongsToProviderException.class);
    }

    @Test
    @Tag("PRV-6")
    @DisplayName("Only the provider's own owner joins as its owner: Siti can't be added as Kemas Home's owner.")
    void onlyTheOwnerJoinsAsTheOwner() {
        Provider kemasHome = openedKemasHome();

        assertThatThrownBy(() -> kemasHome.addOwnerToTeam(
                new TeamMemberId("member-siti"), TeamRole.HELPER, sitiVerified(), Affiliation.none(), TEAM_ADDED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(kemasHome.id().value())
                .hasMessageContaining("person-siti");
    }

    @Test
    @Tag("PRV-6")
    @Tag("HLP-1")
    @DisplayName("The owner's member needs the same checks as a local person: one who didn't pass the age check is refused.")
    void ownerMemberNeedsTheAgeCheck() {
        Provider hanaCleaning = openedHanaCleaning();
        VerifiedPersonFacts underage = VerifiedPersonFacts.local(HANA, false, VERIFIED_UNTIL);

        assertThatThrownBy(() -> hanaCleaning.addOwnerToTeam(
                HANA_MEMBER, TeamRole.HELPER, underage, HANA_OWNS_HANA_CLEANING, TEAM_ADDED_AT))
                .isInstanceOf(MemberUnderageException.class);
    }
}
