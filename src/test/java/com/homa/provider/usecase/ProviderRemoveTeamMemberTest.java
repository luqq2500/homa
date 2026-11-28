package com.homa.provider.usecase;

import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.provider.usecase.KemasHomeScenario.NURUL_MEMBER;
import static com.homa.provider.usecase.KemasHomeScenario.RINA_MEMBER;
import static com.homa.provider.usecase.KemasHomeScenario.SITI_MEMBER;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.AZMAN;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.HANA;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.KEMAS_HOME;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.RINA;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SATURDAY_CHECK_IN_START;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SATURDAY_JOB_END;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SATURDAY_RESERVATION;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SITI;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.cancelled;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.completed;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.rosterChanged;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.saturdayReserved;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.PersonVerifiedIE;
import com.homa.ie.events.ProviderBookabilityChangedIE;
import com.homa.ie.events.TeamMemberActivatedIE;
import com.homa.ie.events.TeamMemberDeactivatedIE;
import com.homa.ie.port.ProviderIE;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.domain.exception.MemberInRosteredWindowException;
import com.homa.provider.domain.exception.MemberRemovedException;
import com.homa.provider.usecase.dto.AddTeamMemberRequest;
import com.homa.provider.usecase.dto.AddTeamMemberResponse;
import com.homa.provider.usecase.dto.RemoveTeamMemberRequest;
import com.homa.provider.usecase.dto.RemoveTeamMemberResponse;
import com.homa.provider.usecase.exception.NotAllowedException;
import com.homa.provider.usecase.exception.TeamMemberNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderRemoveTeamMemberTest {

    private static final Instant WEDNESDAY = at(2026, 12, 2, 15, 0);

    private final ProviderWorld world = new ProviderWorld();

    @BeforeEach
    void kemasHomeIsLiveWithItsTeamAndRina() {
        KemasHomeScenario.liveWithSitiNurulAndFarid(world);
        KemasHomeScenario.withRina(world);
        world.time.setNow(WEDNESDAY);
    }

    private RemoveTeamMemberResponse azmanRemoves(String memberId) {
        return world.removeTeamMember.remove(new RemoveTeamMemberRequest(AZMAN, KEMAS_HOME.value(), memberId));
    }

    /** Saturday's job is Reserved and Kemas Home rosters Siti and Rina on it. */
    private void saturdayIsRosteredOnSitiAndRina() {
        world.reservationReserved.handle(saturdayReserved("reserved-saturday"));
        world.rosterChanged.handle(rosterChanged("roster-saturday", SATURDAY_RESERVATION, SITI_MEMBER, RINA_MEMBER));
    }

    @Test
    @Tag("HLP-8")
    @DisplayName("Rina resigns on Wednesday and is removed the same day: TeamMemberDeactivatedIE says REMOVED.")
    void ownerRemovesAMemberWhoResigns() {
        RemoveTeamMemberResponse response = azmanRemoves(RINA_MEMBER);

        assertThat(response.memberId()).isEqualTo(RINA_MEMBER);
        assertThat(response.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(response.personId()).isEqualTo(RINA);
        assertThat(response.providerBookable()).isTrue();
        TeamMember rina = world.members.stored(new TeamMemberId(RINA_MEMBER));
        assertThat(rina.isRemoved()).isTrue();
        assertThat(rina.isActive()).isFalse();

        TeamMemberDeactivatedIE event = world.publisher.single(TeamMemberDeactivatedIE.class);
        assertThat(event.eventId()).isEqualTo("event-1");
        assertThat(event.memberId()).isEqualTo(RINA_MEMBER);
        assertThat(event.personId()).isEqualTo(RINA);
        assertThat(event.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(event.reason()).isEqualTo("REMOVED");
        assertThat(event.occurredAt()).isEqualTo(WEDNESDAY);
    }

    @Test
    @Tag("HLP-8")
    @DisplayName("Rina, removed, can't be removed again or reactivated; she can only come back as a new member.")
    void removalIsFinal() {
        azmanRemoves(RINA_MEMBER);

        assertThatThrownBy(() -> azmanRemoves(RINA_MEMBER)).isInstanceOf(MemberRemovedException.class);
        assertThat(world.publisher.published()).hasSize(1);
    }

    @Test
    @Tag("HLP-8")
    @Tag("HLP-10")
    @DisplayName("Rina resigns Wednesday and is removed the same day; a year later she's added and verified as new, a new member.")
    void removedMemberIsAddedAsNewAYearLater() {
        azmanRemoves(RINA_MEMBER);
        Instant aYearLater = at(2027, 12, 2, 10, 0);
        Instant nextDue = at(2028, 6, 2, 9, 0);
        world.time.setNow(aYearLater);
        world.personVerified.handle(new PersonVerifiedIE("verification-rina-new", RINA, "MEMBER", null, "Rina",
                "FOREIGN", true, "2028-10-01", nextDue, aYearLater));

        AddTeamMemberResponse response = world.addTeamMember.add(
                new AddTeamMemberRequest(AZMAN, KEMAS_HOME.value(), RINA, "HELPER", true));

        assertThat(response.memberId()).isNotEqualTo(RINA_MEMBER);
        assertThat(response.personId()).isEqualTo(RINA);
        assertThat(world.members.stored(new TeamMemberId(response.memberId())).isActive()).isTrue();
        assertThat(world.members.stored(new TeamMemberId(response.memberId())).verifiedUntil()).isEqualTo(nextDue);
        assertThat(world.members.stored(new TeamMemberId(RINA_MEMBER)).isRemoved()).isTrue();
        assertThat(world.members.findByPersonId(new PersonId(RINA))).hasSize(2);
        List<ProviderIE> published = world.publisher.published();
        assertThat(published).hasSize(2);
        assertThat(((TeamMemberActivatedIE) published.get(1)).memberId()).isEqualTo(response.memberId());
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("Rina quits at 11:00 during a job: Kemas Home can't remove her then, and nothing changes.")
    void memberCantBeRemovedDuringTheirJob() {
        saturdayIsRosteredOnSitiAndRina();
        world.time.setNow(at(2026, 12, 5, 11, 0));

        assertThatThrownBy(() -> azmanRemoves(RINA_MEMBER))
                .isInstanceOf(MemberInRosteredWindowException.class)
                .hasMessageContaining(RINA_MEMBER);

        assertThat(world.members.stored(new TeamMemberId(RINA_MEMBER)).isRemoved()).isFalse();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("Kemas Home reports Roster Member Left and removes Rina at 14:00, when the job has ended.")
    void memberIsRemovedWhenTheJobEnds() {
        saturdayIsRosteredOnSitiAndRina();
        world.time.setNow(SATURDAY_JOB_END);

        azmanRemoves(RINA_MEMBER);

        assertThat(world.members.stored(new TeamMemberId(RINA_MEMBER)).isRemoved()).isTrue();
        assertThat(world.publisher.single(TeamMemberDeactivatedIE.class).reason()).isEqualTo("REMOVED");
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("One minute before the job ends Rina is still inside it; one minute before the Check-in Period she isn't there yet.")
    void windowBoundariesInTheUseCase() {
        saturdayIsRosteredOnSitiAndRina();

        world.time.setNow(SATURDAY_JOB_END.minus(Duration.ofMinutes(1)));
        assertThatThrownBy(() -> azmanRemoves(RINA_MEMBER)).isInstanceOf(MemberInRosteredWindowException.class);
        world.time.setNow(SATURDAY_CHECK_IN_START);
        assertThatThrownBy(() -> azmanRemoves(RINA_MEMBER)).isInstanceOf(MemberInRosteredWindowException.class);
        world.time.setNow(SATURDAY_CHECK_IN_START.minus(Duration.ofMinutes(1)));
        azmanRemoves(RINA_MEMBER);

        assertThat(world.members.stored(new TeamMemberId(RINA_MEMBER)).isRemoved()).isTrue();
    }

    @Test
    @Tag("HLP-9")
    @Tag("ROS-5")
    @DisplayName("Had Rina resigned on Wednesday, she could be removed then and replaced; Nurul, not rostered, can be removed during the job.")
    void onlyTheRosteredMemberIsHeldByTheWindow() {
        saturdayIsRosteredOnSitiAndRina();
        azmanRemoves(RINA_MEMBER);
        world.time.setNow(at(2026, 12, 5, 11, 0));

        azmanRemoves(NURUL_MEMBER);

        assertThat(world.members.stored(new TeamMemberId(RINA_MEMBER)).isRemoved()).isTrue();
        assertThat(world.members.stored(new TeamMemberId(NURUL_MEMBER)).isRemoved()).isTrue();
        assertThat(world.members.stored(new TeamMemberId(SITI_MEMBER)).isRemoved()).isFalse();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("Once Saturday's job is Completed it holds nobody, so Rina can be removed even at what was job time.")
    void completedReservationNoLongerHoldsItsRoster() {
        saturdayIsRosteredOnSitiAndRina();
        world.reservationCompleted.handle(completed("completed-saturday", SATURDAY_RESERVATION));
        world.time.setNow(at(2026, 12, 5, 11, 0));

        azmanRemoves(RINA_MEMBER);

        assertThat(world.members.stored(new TeamMemberId(RINA_MEMBER)).isRemoved()).isTrue();
    }

    @Test
    @Tag("HLP-9")
    @DisplayName("Once Saturday's job is Cancelled it holds nobody either.")
    void cancelledReservationNoLongerHoldsItsRoster() {
        saturdayIsRosteredOnSitiAndRina();
        world.reservationCancelled.handle(cancelled("cancelled-saturday", SATURDAY_RESERVATION));
        world.time.setNow(at(2026, 12, 5, 11, 0));

        azmanRemoves(RINA_MEMBER);

        assertThat(world.members.stored(new TeamMemberId(RINA_MEMBER)).isRemoved()).isTrue();
    }

    @Test
    @Tag("HLP-9")
    @Tag("ROS-1")
    @DisplayName("Kemas Home swapped Rina off Saturday's roster, so she isn't in its window any more.")
    void memberTakenOffTheRosterIsNotHeld() {
        saturdayIsRosteredOnSitiAndRina();
        world.rosterChanged.handle(rosterChanged("roster-saturday-2", SATURDAY_RESERVATION, SITI_MEMBER,
                NURUL_MEMBER));
        world.time.setNow(at(2026, 12, 5, 11, 0));

        azmanRemoves(RINA_MEMBER);

        assertThat(world.members.stored(new TeamMemberId(RINA_MEMBER)).isRemoved()).isTrue();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Removing the last active helper leaves Kemas Home with none, so it can't be booked: NO_ACTIVE_HELPERS.")
    void removingTheLastHelperTakesTheProviderOffSearch() {
        azmanRemoves(RINA_MEMBER);
        azmanRemoves(SITI_MEMBER);
        RemoveTeamMemberResponse last = azmanRemoves(NURUL_MEMBER);

        assertThat(last.providerBookable()).isFalse();
        List<ProviderIE> published = world.publisher.published();
        assertThat(published).hasSize(4);
        ProviderBookabilityChangedIE bookability = (ProviderBookabilityChangedIE) published.get(3);
        assertThat(bookability.eventId()).isEqualTo("event-4");
        assertThat(bookability.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(bookability.bookable()).isFalse();
        assertThat(bookability.reason()).isEqualTo("NO_ACTIVE_HELPERS");
        assertThat(bookability.occurredAt()).isEqualTo(WEDNESDAY);
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Only Azman runs Kemas Home's team: Hana can't remove Rina, and an unknown member is not found.")
    void onlyTheOwnerRemoves() {
        assertThatThrownBy(() -> world.removeTeamMember.remove(
                new RemoveTeamMemberRequest(HANA, KEMAS_HOME.value(), RINA_MEMBER)))
                .isInstanceOf(NotAllowedException.class);
        assertThatThrownBy(() -> azmanRemoves("member-unknown")).isInstanceOf(TeamMemberNotFoundException.class);

        assertThat(world.members.stored(new TeamMemberId(RINA_MEMBER)).isRemoved()).isFalse();
        assertThat(world.publisher.published()).isEmpty();
    }
}
