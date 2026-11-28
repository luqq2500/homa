package com.homa.provider.usecase;

import static com.homa.provider.usecase.ProviderUsecaseFixtures.AZMAN;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.FARID;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.HANA;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.KEMAS_HOME;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.NEXT_DUE_AT;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.RINA;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.RINA_PERMIT_EXPIRY;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.RINA_ROSTERABLE_UNTIL;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SITI;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.TEAM_ADDED_AT;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.kemasHome;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.verifiedLocal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ProviderBookabilityChangedIE;
import com.homa.ie.events.TeamMemberActivatedIE;
import com.homa.ie.port.ProviderIE;
import com.homa.provider.domain.Eligibility.ForeignWorker;
import com.homa.provider.domain.Eligibility.Local;
import com.homa.provider.domain.Owner;
import com.homa.provider.domain.PayoutAccountHolder;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.ProviderId;
import com.homa.provider.domain.ProviderStatus;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.domain.TeamRole;
import com.homa.provider.domain.VerifiedPersonFacts;
import com.homa.provider.domain.exception.EmploymentNotConfirmedException;
import com.homa.provider.domain.exception.MemberUnderageException;
import com.homa.provider.domain.exception.PersonAlreadyBelongsToProviderException;
import com.homa.provider.usecase.dto.AddTeamMemberRequest;
import com.homa.provider.usecase.dto.AddTeamMemberResponse;
import com.homa.provider.usecase.dto.DeactivateTeamMemberRequest;
import com.homa.provider.usecase.dto.RemoveTeamMemberRequest;
import com.homa.provider.usecase.exception.NotAllowedException;
import com.homa.provider.usecase.exception.PersonNotVerifiedException;
import com.homa.provider.usecase.exception.ProviderNotFoundException;
import com.homa.provider.usecase.port.out.VerifiedPerson;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderAddTeamMemberTest {

    private static final ProviderId HANA_CLEANING = new ProviderId("provider-hana-cleaning");

    private final ProviderWorld world = new ProviderWorld();

    @BeforeEach
    void kemasHomeIsOpenAndItsPeopleAreVerified() {
        world.providers.save(kemasHome(ProviderStatus.OPENED, null));
        world.verifiedPeople.save(verifiedLocal(SITI, "Siti"));
        world.verifiedPeople.save(verifiedLocal(FARID, "Farid"));
        world.verifiedPeople.save(new VerifiedPerson(
                VerifiedPersonFacts.foreign(new PersonId(RINA), true, RINA_PERMIT_EXPIRY, NEXT_DUE_AT), "Rina"));
    }

    private AddTeamMemberResponse azmanAdds(String personId, String role, boolean employmentConfirmed) {
        return world.addTeamMember.add(
                new AddTeamMemberRequest(AZMAN, KEMAS_HOME.value(), personId, role, employmentConfirmed));
    }

    @Test
    @Tag("HLP-1")
    @Tag("HLP-2")
    @DisplayName("Azman adds Siti, verified with her MyKad, as a helper: she is active and TeamMemberActivatedIE says so.")
    void ownerAddsALocalHelper() {
        AddTeamMemberResponse response = azmanAdds(SITI, "HELPER", false);

        assertThat(response.memberId()).isNotBlank();
        assertThat(response.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(response.personId()).isEqualTo(SITI);
        assertThat(response.role()).isEqualTo("HELPER");
        assertThat(response.eligibility()).isEqualTo("LOCAL");
        assertThat(response.photoVerified()).isFalse();
        assertThat(response.rosterableUntil()).isNull();

        TeamMember siti = world.members.stored(new TeamMemberId(response.memberId()));
        assertThat(siti.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(siti.personId()).isEqualTo(new PersonId(SITI));
        assertThat(siti.role()).isEqualTo(TeamRole.HELPER);
        assertThat(siti.eligibility()).isEqualTo(new Local());
        assertThat(siti.addedAt()).isEqualTo(TEAM_ADDED_AT);
        assertThat(siti.isActiveHelper()).isTrue();
        assertThat(siti.hasVerifiedPhoto()).isFalse();

        TeamMemberActivatedIE event = world.publisher.single(TeamMemberActivatedIE.class);
        assertThat(event.eventId()).isEqualTo("event-1");
        assertThat(event.memberId()).isEqualTo(response.memberId());
        assertThat(event.personId()).isEqualTo(SITI);
        assertThat(event.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(event.displayName()).isEqualTo("Siti");
        assertThat(event.role()).isEqualTo("HELPER");
        assertThat(event.eligibility()).isEqualTo("LOCAL");
        assertThat(event.photoVerified()).isFalse();
        assertThat(event.rosterableUntil()).isNull();
        assertThat(event.occurredAt()).isEqualTo(TEAM_ADDED_AT);
    }

    @Test
    @Tag("HLP-3")
    @Tag("HLP-4")
    @Tag("ROS-4")
    @DisplayName("Rina's permit has 200 days left and Kemas Home confirms she works for it, so she's added, rosterable until 20 December.")
    void ownerAddsAForeignHelperWithConfirmation() {
        AddTeamMemberResponse response = azmanAdds(RINA, "HELPER", true);

        assertThat(response.eligibility()).isEqualTo("FOREIGN_WORKER");
        assertThat(response.rosterableUntil()).isEqualTo(RINA_ROSTERABLE_UNTIL);
        assertThat(world.members.stored(new TeamMemberId(response.memberId())).eligibility())
                .isEqualTo(new ForeignWorker(RINA_PERMIT_EXPIRY, true));

        TeamMemberActivatedIE event = world.publisher.single(TeamMemberActivatedIE.class);
        assertThat(event.eventId()).isEqualTo("event-1");
        assertThat(event.memberId()).isEqualTo(response.memberId());
        assertThat(event.personId()).isEqualTo(RINA);
        assertThat(event.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(event.displayName()).isEqualTo("Rina");
        assertThat(event.role()).isEqualTo("HELPER");
        assertThat(event.eligibility()).isEqualTo("FOREIGN_WORKER");
        assertThat(event.photoVerified()).isFalse();
        assertThat(event.rosterableUntil()).isEqualTo("2026-12-20");
        assertThat(event.occurredAt()).isEqualTo(TEAM_ADDED_AT);
    }

    @Test
    @Tag("HLP-4")
    @DisplayName("Without Kemas Home's confirmation that Rina works lawfully for it, she can't be added.")
    void foreignHelperWithoutConfirmationIsRefused() {
        assertThatThrownBy(() -> azmanAdds(RINA, "HELPER", false))
                .isInstanceOf(EmploymentNotConfirmedException.class);
        assertThat(world.members.all()).isEmpty();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("HLP-1")
    @DisplayName("Kemas Home can't add a 17-year-old trainee.")
    void underageTraineeIsRefused() {
        world.verifiedPeople.save(new VerifiedPerson(
                VerifiedPersonFacts.local(new PersonId("person-trainee"), false, NEXT_DUE_AT), "Trainee"));

        assertThatThrownBy(() -> azmanAdds("person-trainee", "HELPER", false))
                .isInstanceOf(MemberUnderageException.class);
        assertThat(world.members.all()).isEmpty();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("IDN-4")
    @Tag("ROS-6")
    @DisplayName("Siti's photo was verified before she joined, so she starts with a verified photo.")
    void memberStartsWithThePhotoVerificationAlreadyReported() {
        world.photos.record(new PersonId(SITI), true);

        AddTeamMemberResponse response = azmanAdds(SITI, "HELPER", false);

        assertThat(response.photoVerified()).isTrue();
        assertThat(world.members.stored(new TeamMemberId(response.memberId())).hasVerifiedPhoto()).isTrue();
        assertThat(world.publisher.single(TeamMemberActivatedIE.class).photoVerified()).isTrue();
    }

    @Test
    @Tag("PRV-3")
    @DisplayName("Kemas Home, already live, adds Farid as a supervisor a month later.")
    void ownerAddsASupervisorAfterGoingLive() {
        world.providers.save(kemasHome(ProviderStatus.LIVE, PayoutAccountHolder.BUSINESS));

        AddTeamMemberResponse response = azmanAdds(FARID, "SUPERVISOR", false);

        assertThat(response.role()).isEqualTo("SUPERVISOR");
        TeamMember farid = world.members.stored(new TeamMemberId(response.memberId()));
        assertThat(farid.role()).isEqualTo(TeamRole.SUPERVISOR);
        assertThat(farid.isActive()).isTrue();
        TeamMemberActivatedIE event = world.publisher.single(TeamMemberActivatedIE.class);
        assertThat(event.role()).isEqualTo("SUPERVISOR");
        assertThat(event.displayName()).isEqualTo("Farid");
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Only Azman runs Kemas Home's account: Hana can't add a member to it.")
    void onlyTheOwnerAddsMembers() {
        assertThatThrownBy(() -> world.addTeamMember.add(
                new AddTeamMemberRequest(HANA, KEMAS_HOME.value(), SITI, "HELPER", false)))
                .isInstanceOf(NotAllowedException.class)
                .hasMessageContaining(HANA)
                .hasMessageContaining(KEMAS_HOME.value());
        assertThat(world.members.all()).isEmpty();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("A member can't be added to a provider that doesn't exist.")
    void unknownProviderIsNotFound() {
        assertThatThrownBy(() -> world.addTeamMember.add(
                new AddTeamMemberRequest(AZMAN, "provider-unknown", SITI, "HELPER", false)))
                .isInstanceOf(ProviderNotFoundException.class)
                .hasMessageContaining("provider-unknown");
    }

    @Test
    @Tag("IDN-1")
    @DisplayName("A candidate who hasn't passed identity verification can't be added.")
    void unverifiedCandidateIsRefused() {
        assertThatThrownBy(() -> azmanAdds("person-candidate", "HELPER", false))
                .isInstanceOf(PersonNotVerifiedException.class)
                .hasMessageContaining("person-candidate");
        assertThat(world.members.all()).isEmpty();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("A role other than HELPER or SUPERVISOR is refused.")
    void unknownRoleIsRefused() {
        assertThatThrownBy(() -> azmanAdds(SITI, "OWNER", false)).isInstanceOf(IllegalArgumentException.class);
        assertThat(world.members.all()).isEmpty();
    }

    private void hanaCleaningIsOpenWithHanaVerified() {
        world.providers.save(Provider.restore(HANA_CLEANING, new Owner(new PersonId(HANA)), ProviderStatus.OPENED, null));
    }

    @Test
    @Tag("HLP-10")
    @DisplayName("Siti, a Kemas Home helper, can't join Hana Cleaning until Kemas Home removes her.")
    void memberOfAnotherProviderCantBeAdded() {
        KemasHomeScenario.liveWithSitiNurulAndFarid(world);
        hanaCleaningIsOpenWithHanaVerified();

        assertThatThrownBy(() -> world.addTeamMember.add(
                new AddTeamMemberRequest(HANA, HANA_CLEANING.value(), SITI, "HELPER", false)))
                .isInstanceOf(PersonAlreadyBelongsToProviderException.class)
                .hasMessageContaining(SITI);
        assertThat(world.members.findByProviderId(HANA_CLEANING)).isEmpty();
        assertThat(world.publisher.published()).isEmpty();

        world.removeTeamMember.remove(
                new RemoveTeamMemberRequest(AZMAN, KEMAS_HOME.value(), KemasHomeScenario.SITI_MEMBER));
        AddTeamMemberResponse response = world.addTeamMember.add(
                new AddTeamMemberRequest(HANA, HANA_CLEANING.value(), SITI, "HELPER", false));

        assertThat(response.providerId()).isEqualTo(HANA_CLEANING.value());
        assertThat(response.personId()).isEqualTo(SITI);
    }

    @Test
    @Tag("HLP-10")
    @DisplayName("Siti is deactivated for leave at Kemas Home: she still belongs to it, so Hana Cleaning can't add her.")
    void deactivatedMemberOfAnotherProviderCantBeAdded() {
        KemasHomeScenario.liveWithSitiNurulAndFarid(world);
        hanaCleaningIsOpenWithHanaVerified();
        world.deactivateTeamMember.deactivate(
                new DeactivateTeamMemberRequest(AZMAN, KEMAS_HOME.value(), KemasHomeScenario.SITI_MEMBER));

        assertThatThrownBy(() -> world.addTeamMember.add(
                new AddTeamMemberRequest(HANA, HANA_CLEANING.value(), SITI, "HELPER", false)))
                .isInstanceOf(PersonAlreadyBelongsToProviderException.class);
    }

    @Test
    @Tag("HLP-10")
    @DisplayName("Siti is on Kemas Home's team already, so Azman can't add her to it a second time.")
    void memberCantBeAddedToTheSameTeamTwice() {
        KemasHomeScenario.liveWithSitiNurulAndFarid(world);

        assertThatThrownBy(() -> azmanAdds(SITI, "HELPER", false))
                .isInstanceOf(PersonAlreadyBelongsToProviderException.class);
        assertThat(world.members.findByProviderId(KEMAS_HOME)).hasSize(3);
    }

    @Test
    @Tag("HLP-10")
    @DisplayName("Azman owns Kemas Home, so Hana can't add him to Hana Cleaning's team.")
    void ownerOfAnotherProviderCantBeAdded() {
        KemasHomeScenario.liveWithSitiNurulAndFarid(world);
        hanaCleaningIsOpenWithHanaVerified();
        world.verifiedPeople.save(verifiedLocal(AZMAN, "Azman"));

        assertThatThrownBy(() -> world.addTeamMember.add(
                new AddTeamMemberRequest(HANA, HANA_CLEANING.value(), AZMAN, "SUPERVISOR", false)))
                .isInstanceOf(PersonAlreadyBelongsToProviderException.class);
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home, live with every helper deactivated, can be booked again as soon as a new helper joins.")
    void addingAHelperToAProviderWithNoActiveHelperMakesItBookable() {
        world.providers.save(kemasHome(ProviderStatus.LIVE, PayoutAccountHolder.BUSINESS));

        AddTeamMemberResponse response = azmanAdds(SITI, "HELPER", false);

        List<ProviderIE> published = world.publisher.published();
        assertThat(published).hasSize(2);
        assertThat(published.get(0)).isInstanceOf(TeamMemberActivatedIE.class);
        ProviderBookabilityChangedIE bookability = (ProviderBookabilityChangedIE) published.get(1);
        assertThat(bookability.eventId()).isEqualTo("event-2");
        assertThat(bookability.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(bookability.bookable()).isTrue();
        assertThat(bookability.reason()).isNull();
        assertThat(bookability.occurredAt()).isEqualTo(TEAM_ADDED_AT);
        assertThat(response.personId()).isEqualTo(SITI);
    }

    @Test
    @Tag("PRV-3")
    @DisplayName("Kemas Home, live with no helper, stays unbookable when only a supervisor joins.")
    void addingASupervisorDoesNotMakeAProviderBookable() {
        world.providers.save(kemasHome(ProviderStatus.LIVE, PayoutAccountHolder.BUSINESS));

        azmanAdds(FARID, "SUPERVISOR", false);

        assertThat(world.publisher.published()).hasSize(1).allMatch(TeamMemberActivatedIE.class::isInstance);
    }
}
