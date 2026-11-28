package com.homa.provider.usecase;

import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.provider.usecase.KemasHomeScenario.FARID_MEMBER;
import static com.homa.provider.usecase.KemasHomeScenario.NURUL_MEMBER;
import static com.homa.provider.usecase.KemasHomeScenario.SITI_MEMBER;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.AZMAN;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.FARID;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.HANA;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.KEMAS_HOME;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.NURUL;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.NEXT_DUE_AT;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SITI;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.TEAM_ADDED_AT;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.kemasHome;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.storedMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ProviderBookabilityChangedIE;
import com.homa.ie.events.TeamMemberDeactivatedIE;
import com.homa.ie.port.ProviderIE;
import com.homa.provider.domain.Eligibility.Local;
import com.homa.provider.domain.Owner;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.ProviderId;
import com.homa.provider.domain.ProviderStatus;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.domain.TeamRole;
import com.homa.provider.domain.exception.MemberAlreadyDeactivatedException;
import com.homa.provider.domain.exception.MemberRemovedException;
import com.homa.provider.usecase.dto.DeactivateTeamMemberRequest;
import com.homa.provider.usecase.dto.DeactivateTeamMemberResponse;
import com.homa.provider.usecase.exception.NotAllowedException;
import com.homa.provider.usecase.exception.ProviderNotFoundException;
import com.homa.provider.usecase.exception.TeamMemberNotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderDeactivateTeamMemberTest {

    private static final Instant THURSDAY = at(2026, 12, 3, 10, 0);

    private final ProviderWorld world = new ProviderWorld();

    @BeforeEach
    void kemasHomeIsLiveWithItsTeam() {
        KemasHomeScenario.liveWithSitiNurulAndFarid(world);
        world.time.setNow(THURSDAY);
    }

    private DeactivateTeamMemberResponse azmanDeactivates(String memberId) {
        return world.deactivateTeamMember.deactivate(
                new DeactivateTeamMemberRequest(AZMAN, KEMAS_HOME.value(), memberId));
    }

    @Test
    @Tag("HLP-5")
    @DisplayName("Siti is deactivated for two weeks' leave: she isn't active, and TeamMemberDeactivatedIE says DEACTIVATED.")
    void ownerDeactivatesAMemberForLeave() {
        DeactivateTeamMemberResponse response = azmanDeactivates(SITI_MEMBER);

        assertThat(response.memberId()).isEqualTo(SITI_MEMBER);
        assertThat(response.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(response.personId()).isEqualTo(SITI);
        assertThat(response.providerBookable()).isTrue();
        assertThat(world.members.stored(new TeamMemberId(SITI_MEMBER)).isDeactivated()).isTrue();
        assertThat(world.members.stored(new TeamMemberId(SITI_MEMBER)).isActive()).isFalse();
        assertThat(world.members.stored(new TeamMemberId(NURUL_MEMBER)).isActive()).isTrue();

        TeamMemberDeactivatedIE event = world.publisher.single(TeamMemberDeactivatedIE.class);
        assertThat(event.eventId()).isEqualTo("event-1");
        assertThat(event.memberId()).isEqualTo(SITI_MEMBER);
        assertThat(event.personId()).isEqualTo(SITI);
        assertThat(event.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(event.reason()).isEqualTo("DEACTIVATED");
        assertThat(event.occurredAt()).isEqualTo(THURSDAY);
    }

    @Test
    @Tag("HLP-6")
    @DisplayName("Nurul, deactivated on Thursday, can't be rostered on Saturday: Reservation hears she's inactive, so Kemas Home must replace her.")
    void deactivatedMemberIsAnnouncedSoShesNotRostered() {
        azmanDeactivates(NURUL_MEMBER);

        TeamMemberDeactivatedIE event = world.publisher.single(TeamMemberDeactivatedIE.class);
        assertThat(event.memberId()).isEqualTo(NURUL_MEMBER);
        assertThat(event.personId()).isEqualTo(NURUL);
        assertThat(event.reason()).isEqualTo("DEACTIVATED");
        assertThat(world.members.stored(new TeamMemberId(NURUL_MEMBER)).isActiveHelper()).isFalse();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("When all its helpers are deactivated for a holiday week, Kemas Home disappears from search: ProviderBookabilityChangedIE says NO_ACTIVE_HELPERS.")
    void providerDisappearsFromSearchWhenItsLastHelperIsDeactivated() {
        azmanDeactivates(SITI_MEMBER);
        DeactivateTeamMemberResponse last = azmanDeactivates(NURUL_MEMBER);

        assertThat(last.providerBookable()).isFalse();
        List<ProviderIE> published = world.publisher.published();
        assertThat(published).hasSize(3);
        assertThat(published.get(0)).isInstanceOf(TeamMemberDeactivatedIE.class);
        TeamMemberDeactivatedIE nurulDeactivated = (TeamMemberDeactivatedIE) published.get(1);
        assertThat(nurulDeactivated.eventId()).isEqualTo("event-2");
        assertThat(nurulDeactivated.memberId()).isEqualTo(NURUL_MEMBER);
        ProviderBookabilityChangedIE bookability = (ProviderBookabilityChangedIE) published.get(2);
        assertThat(bookability.eventId()).isEqualTo("event-3");
        assertThat(bookability.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(bookability.bookable()).isFalse();
        assertThat(bookability.reason()).isEqualTo("NO_ACTIVE_HELPERS");
        assertThat(bookability.occurredAt()).isEqualTo(THURSDAY);
    }

    @Test
    @Tag("PRV-2")
    @Tag("PRV-3")
    @DisplayName("Deactivating Farid, a supervisor, doesn't change whether Kemas Home can be booked: supervisors don't count.")
    void deactivatingASupervisorLeavesBookabilityAlone() {
        DeactivateTeamMemberResponse response = azmanDeactivates(FARID_MEMBER);

        assertThat(response.providerBookable()).isTrue();
        TeamMemberDeactivatedIE event = world.publisher.single(TeamMemberDeactivatedIE.class);
        assertThat(event.memberId()).isEqualTo(FARID_MEMBER);
        assertThat(event.personId()).isEqualTo(FARID);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A provider that hasn't gone live isn't bookable before or after, so deactivating its helper publishes no bookability change.")
    void providerNotLiveHasNoBookabilityToChange() {
        world.providers.save(kemasHome(ProviderStatus.OPENED, null));

        DeactivateTeamMemberResponse response = azmanDeactivates(SITI_MEMBER);
        azmanDeactivates(NURUL_MEMBER);

        assertThat(response.providerBookable()).isFalse();
        assertThat(world.publisher.published()).hasSize(2).allMatch(TeamMemberDeactivatedIE.class::isInstance);
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Only Azman runs Kemas Home's team: Hana can't deactivate Siti.")
    void onlyTheOwnerDeactivates() {
        assertThatThrownBy(() -> world.deactivateTeamMember.deactivate(
                new DeactivateTeamMemberRequest(HANA, KEMAS_HOME.value(), SITI_MEMBER)))
                .isInstanceOf(NotAllowedException.class);
        assertThatThrownBy(() -> world.deactivateTeamMember.deactivate(
                new DeactivateTeamMemberRequest(SITI, KEMAS_HOME.value(), SITI_MEMBER)))
                .isInstanceOf(NotAllowedException.class);

        assertThat(world.members.stored(new TeamMemberId(SITI_MEMBER)).isDeactivated()).isFalse();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("A member who isn't in the provider's team is not found, and neither is a provider that doesn't exist.")
    void unknownMemberOrProviderIsNotFound() {
        world.members.save(storedMember("member-other", "person-other",
                TeamRole.HELPER, false, false, false));

        assertThatThrownBy(() -> azmanDeactivates("member-unknown"))
                .isInstanceOf(TeamMemberNotFoundException.class)
                .hasMessageContaining("member-unknown");
        assertThatThrownBy(() -> world.deactivateTeamMember.deactivate(
                new DeactivateTeamMemberRequest(AZMAN, "provider-unknown", SITI_MEMBER)))
                .isInstanceOf(ProviderNotFoundException.class);
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("A member of another provider's team is not found: Azman can't deactivate someone who isn't on his team.")
    void memberOfAnotherProviderIsNotFound() {
        world.providers.save(Provider.restore(
                new ProviderId("provider-hana-cleaning"),
                new Owner(new PersonId(HANA)),
                ProviderStatus.LIVE, null));
        world.members.save(TeamMember.restore(new TeamMemberId("member-hana"),
                new ProviderId("provider-hana-cleaning"),
                new PersonId(HANA), TeamRole.HELPER,
                new Local(), TEAM_ADDED_AT, true, false,
                false, false, NEXT_DUE_AT));

        assertThatThrownBy(() -> azmanDeactivates("member-hana")).isInstanceOf(TeamMemberNotFoundException.class);
        assertThat(world.members.stored(new TeamMemberId("member-hana")).isDeactivated()).isFalse();
    }

    @Test
    @Tag("HLP-5")
    @DisplayName("Siti is already deactivated, so deactivating her again is refused and announces nothing.")
    void deactivatingTwiceIsRefused() {
        azmanDeactivates(SITI_MEMBER);

        assertThatThrownBy(() -> azmanDeactivates(SITI_MEMBER)).isInstanceOf(MemberAlreadyDeactivatedException.class);
        assertThat(world.publisher.published()).hasSize(1);
    }

    @Test
    @Tag("HLP-8")
    @DisplayName("A removed member can't be deactivated.")
    void removedMemberCantBeDeactivated() {
        world.members.save(storedMember(
                "member-rina", "person-rina", TeamRole.HELPER, false, false, true));

        assertThatThrownBy(() -> azmanDeactivates("member-rina")).isInstanceOf(MemberRemovedException.class);
        assertThat(world.publisher.published()).isEmpty();
    }
}
