package com.homa.provider.usecase;

import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.provider.usecase.KemasHomeScenario.FARID_MEMBER;
import static com.homa.provider.usecase.KemasHomeScenario.NURUL_MEMBER;
import static com.homa.provider.usecase.KemasHomeScenario.SITI_MEMBER;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.AZMAN;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.FARID;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.HANA;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.KEMAS_HOME;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.NEXT_DUE_AT;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SITI;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.storedMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.PersonVerifiedIE;
import com.homa.ie.events.ProviderBookabilityChangedIE;
import com.homa.ie.events.TeamMemberActivatedIE;
import com.homa.ie.port.ProviderIE;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.domain.TeamRole;
import com.homa.provider.domain.exception.MemberNotDeactivatedException;
import com.homa.provider.domain.exception.MemberRemovedException;
import com.homa.provider.domain.exception.VerificationLapsedException;
import com.homa.provider.usecase.dto.DeactivateTeamMemberRequest;
import com.homa.provider.usecase.dto.ReactivateTeamMemberRequest;
import com.homa.provider.usecase.dto.ReactivateTeamMemberResponse;
import com.homa.provider.usecase.exception.NotAllowedException;
import com.homa.provider.usecase.exception.PersonNotVerifiedException;
import com.homa.provider.usecase.exception.TeamMemberNotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderReactivateTeamMemberTest {

    private static final Instant LEAVE_STARTS = at(2026, 12, 14, 8, 0);
    private static final Instant BACK_FROM_LEAVE = at(2026, 12, 28, 8, 0);

    private final ProviderWorld world = new ProviderWorld();

    @BeforeEach
    void kemasHomeIsLiveWithItsTeam() {
        KemasHomeScenario.liveWithSitiNurulAndFarid(world);
        world.time.setNow(LEAVE_STARTS);
    }

    private void azmanDeactivates(String memberId) {
        world.deactivateTeamMember.deactivate(new DeactivateTeamMemberRequest(AZMAN, KEMAS_HOME.value(), memberId));
    }

    private ReactivateTeamMemberResponse azmanReactivates(String memberId) {
        return world.reactivateTeamMember.reactivate(
                new ReactivateTeamMemberRequest(AZMAN, KEMAS_HOME.value(), memberId));
    }

    @Test
    @Tag("HLP-5")
    @DisplayName("Siti is deactivated for two weeks' leave and reactivated when she's back: TeamMemberActivatedIE says she is active again.")
    void ownerReactivatesAMemberWhoIsBack() {
        azmanDeactivates(SITI_MEMBER);
        world.time.setNow(BACK_FROM_LEAVE);

        ReactivateTeamMemberResponse response = azmanReactivates(SITI_MEMBER);

        assertThat(response.memberId()).isEqualTo(SITI_MEMBER);
        assertThat(response.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(response.personId()).isEqualTo(SITI);
        assertThat(response.providerBookable()).isTrue();
        assertThat(world.members.stored(new TeamMemberId(SITI_MEMBER)).isDeactivated()).isFalse();
        assertThat(world.members.stored(new TeamMemberId(SITI_MEMBER)).isActiveHelper()).isTrue();

        List<ProviderIE> published = world.publisher.published();
        assertThat(published).hasSize(2);
        TeamMemberActivatedIE event = (TeamMemberActivatedIE) published.get(1);
        assertThat(event.eventId()).isEqualTo("event-2");
        assertThat(event.memberId()).isEqualTo(SITI_MEMBER);
        assertThat(event.personId()).isEqualTo(SITI);
        assertThat(event.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(event.displayName()).isEqualTo("Siti");
        assertThat(event.role()).isEqualTo("HELPER");
        assertThat(event.eligibility()).isEqualTo("LOCAL");
        assertThat(event.photoVerified()).isTrue();
        assertThat(event.rosterableUntil()).isNull();
        assertThat(event.occurredAt()).isEqualTo(BACK_FROM_LEAVE);
    }

    @Test
    @Tag("HLP-7")
    @DisplayName("Farid's re-verification fell due while he was deactivated, so he re-verifies before Kemas Home reactivates him.")
    void memberReactivatesOnlyAfterReVerifying() {
        azmanDeactivates(FARID_MEMBER);
        Instant afterItFellDue = NEXT_DUE_AT.plusSeconds(3 * 86_400);
        world.time.setNow(afterItFellDue);

        assertThatThrownBy(() -> azmanReactivates(FARID_MEMBER)).isInstanceOf(VerificationLapsedException.class);
        assertThat(world.members.stored(new TeamMemberId(FARID_MEMBER)).isDeactivated()).isTrue();
        assertThat(world.publisher.published()).hasSize(1);

        Instant nextDue = at(2027, 8, 25, 9, 0);
        world.personVerified.handle(new PersonVerifiedIE("verification-farid-again", FARID, "MEMBER", null, "Farid",
                "LOCAL", true, null, nextDue, afterItFellDue));
        ReactivateTeamMemberResponse response = azmanReactivates(FARID_MEMBER);

        assertThat(response.personId()).isEqualTo(FARID);
        assertThat(world.members.stored(new TeamMemberId(FARID_MEMBER)).isActive()).isTrue();
        assertThat(world.members.stored(new TeamMemberId(FARID_MEMBER)).verifiedUntil()).isEqualTo(nextDue);
        TeamMemberActivatedIE event = (TeamMemberActivatedIE) world.publisher.published().get(1);
        assertThat(event.memberId()).isEqualTo(FARID_MEMBER);
        assertThat(event.role()).isEqualTo("SUPERVISOR");
        assertThat(event.displayName()).isEqualTo("Farid");
    }

    @Test
    @Tag("HLP-7")
    @DisplayName("A verification is valid until it falls due: one second before Siti is reactivated, at the moment itself she isn't.")
    void verificationBoundaryDecidesReactivation() {
        azmanDeactivates(SITI_MEMBER);
        azmanDeactivates(NURUL_MEMBER);

        world.time.setNow(NEXT_DUE_AT);
        assertThatThrownBy(() -> azmanReactivates(SITI_MEMBER)).isInstanceOf(VerificationLapsedException.class);
        world.time.setNow(NEXT_DUE_AT.minusSeconds(1));
        azmanReactivates(NURUL_MEMBER);

        assertThat(world.members.stored(new TeamMemberId(SITI_MEMBER)).isDeactivated()).isTrue();
        assertThat(world.members.stored(new TeamMemberId(NURUL_MEMBER)).isDeactivated()).isFalse();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home came back from its holiday week: when its first helper is reactivated it shows in search again.")
    void providerBecomesBookableAgainWhenAHelperIsBack() {
        azmanDeactivates(SITI_MEMBER);
        azmanDeactivates(NURUL_MEMBER);
        world.time.setNow(BACK_FROM_LEAVE);

        ReactivateTeamMemberResponse response = azmanReactivates(SITI_MEMBER);

        assertThat(response.providerBookable()).isTrue();
        List<ProviderIE> published = world.publisher.published();
        assertThat(published).hasSize(5);
        assertThat(published.get(3)).isInstanceOf(TeamMemberActivatedIE.class);
        ProviderBookabilityChangedIE bookability = (ProviderBookabilityChangedIE) published.get(4);
        assertThat(bookability.eventId()).isEqualTo("event-5");
        assertThat(bookability.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(bookability.bookable()).isTrue();
        assertThat(bookability.reason()).isNull();
        assertThat(bookability.occurredAt()).isEqualTo(BACK_FROM_LEAVE);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Reactivating a second helper changes nothing about whether Kemas Home can be booked, so it publishes no bookability change.")
    void secondHelperBackLeavesBookabilityAlone() {
        azmanDeactivates(SITI_MEMBER);
        azmanDeactivates(NURUL_MEMBER);
        world.time.setNow(BACK_FROM_LEAVE);
        azmanReactivates(SITI_MEMBER);

        azmanReactivates(NURUL_MEMBER);

        assertThat(world.publisher.published()).hasSize(6);
        assertThat(world.publisher.published().get(5)).isInstanceOf(TeamMemberActivatedIE.class);
    }

    @Test
    @Tag("HLP-5")
    @DisplayName("Siti is active, so there is nothing to reactivate.")
    void activeMemberCantBeReactivated() {
        assertThatThrownBy(() -> azmanReactivates(SITI_MEMBER)).isInstanceOf(MemberNotDeactivatedException.class);
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("HLP-8")
    @DisplayName("Rina was removed, so she can't be reactivated: she can only come back as a new member.")
    void removedMemberCantBeReactivated() {
        world.members.save(storedMember("member-rina", "person-rina", TeamRole.HELPER, true, false, true));
        world.verifiedPeople.save(ProviderUsecaseFixtures.verifiedLocal("person-rina", "Rina"));

        assertThatThrownBy(() -> azmanReactivates("member-rina")).isInstanceOf(MemberRemovedException.class);
        assertThat(world.members.stored(new TeamMemberId("member-rina")).isRemoved()).isTrue();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("HLP-5")
    @DisplayName("Nurul is deactivated and also suspended: reactivating her lifts the provider's deactivation, but she isn't announced as active.")
    void suspendedMemberIsNotAnnouncedAsActive() {
        world.members.save(storedMember(NURUL_MEMBER, "person-nurul", TeamRole.HELPER, true, true, false));
        world.time.setNow(BACK_FROM_LEAVE);

        ReactivateTeamMemberResponse response = azmanReactivates(NURUL_MEMBER);

        assertThat(response.providerBookable()).isTrue();
        assertThat(world.members.stored(new TeamMemberId(NURUL_MEMBER)).isDeactivated()).isFalse();
        assertThat(world.members.stored(new TeamMemberId(NURUL_MEMBER)).isActive()).isFalse();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("IDN-1")
    @DisplayName("A member whose verification Provider doesn't hold can't be announced, so reactivating them is refused.")
    void memberWithoutAVerificationIsRefused() {
        world.members.save(storedMember("member-stranger", "person-stranger", TeamRole.HELPER, true, false, false));

        assertThatThrownBy(() -> azmanReactivates("member-stranger"))
                .isInstanceOf(PersonNotVerifiedException.class)
                .hasMessageContaining("person-stranger");
        assertThat(world.members.stored(new TeamMemberId("member-stranger")).isDeactivated()).isTrue();
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Only Azman runs Kemas Home's team: Hana can't reactivate Siti, and an unknown member is not found.")
    void onlyTheOwnerReactivatesAMemberOfHisTeam() {
        azmanDeactivates(SITI_MEMBER);

        assertThatThrownBy(() -> world.reactivateTeamMember.reactivate(
                new ReactivateTeamMemberRequest(HANA, KEMAS_HOME.value(), SITI_MEMBER)))
                .isInstanceOf(NotAllowedException.class);
        assertThatThrownBy(() -> azmanReactivates("member-unknown")).isInstanceOf(TeamMemberNotFoundException.class);
        assertThat(world.members.stored(new TeamMemberId(SITI_MEMBER)).isDeactivated()).isTrue();
        assertThat(world.publisher.published()).hasSize(1);
    }
}
