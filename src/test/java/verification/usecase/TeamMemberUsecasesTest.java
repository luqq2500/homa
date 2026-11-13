package verification.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static verification.support.VerificationFixtures.NEXT_YEAR;
import static verification.support.VerificationFixtures.NOW;
import static verification.support.VerificationFixtures.PHOTO;

import ie.events.TeamMemberCheckFailedIE;
import ie.events.TeamMemberJoinedIE;
import ie.events.TeamMemberLeftIE;
import ie.events.TeamMemberRestoredIE;
import ie.events.TeamMemberReverifiedIE;
import ie.events.TeamMemberRoleChangedIE;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import verification.domain.Role;
import verification.domain.TeamMemberStatus;
import verification.domain.exception.BusinessNotVerifiedException;
import verification.domain.exception.EmploymentNotConfirmedException;
import verification.support.VerificationWorld;
import verification.usecase.dto.ChangeTeamMemberRoleRequest;
import verification.usecase.dto.ConfirmTeamMemberRequest;
import verification.usecase.dto.ConfirmTeamMemberResponse;
import verification.usecase.dto.RemoveTeamMemberByHomaRequest;
import verification.usecase.dto.RemoveTeamMemberRequest;
import verification.usecase.dto.ReportTeamMemberLapseRequest;
import verification.usecase.dto.ReportTeamMemberResultRequest;
import verification.usecase.dto.ReportTeamMemberReverificationNeededRequest;
import verification.usecase.dto.StartTeamMemberRequest;
import verification.usecase.dto.StartTeamMemberResponse;
import verification.usecase.dto.StartTeamMemberReverificationRequest;
import verification.usecase.dto.ViewVerificationRequest;
import verification.usecase.dto.ViewVerificationResponse;
import verification.usecase.dto.ViewVerificationResponse.Person;
import verification.usecase.exception.TeamMemberNotFoundException;
import verification.usecase.exception.UnknownCheckFailureException;
import verification.usecase.exception.UnknownRoleException;

class TeamMemberUsecasesTest {

    private static final String BUSINESS = "business-1";

    private final VerificationWorld world = new VerificationWorld();
    private final ProviderStartTeamMember starter =
            new ProviderStartTeamMember(world.businesses, world.members, world.kyc);
    private final KycReportTeamMemberResult results =
            new KycReportTeamMemberResult(world.members, world.publisher, world.clock);
    private final ProviderConfirmTeamMember confirmer =
            new ProviderConfirmTeamMember(world.members, world.publisher, world.clock);

    TeamMemberUsecasesTest() {
        world.verifiedBusiness(BUSINESS);
    }

    @Test
    void startingAPersonOpensTheirCheckWithTheKycProvider() {
        StartTeamMemberResponse response = starter.start(new StartTeamMemberRequest(BUSINESS, "SUPERVISOR", PHOTO));

        assertThat(response.session()).isEqualTo("session-for-" + response.memberId());
        assertThat(world.kycCalls).containsExactly("member-check " + response.memberId() + " " + PHOTO);
        assertThat(world.members.find(response.memberId())).get().satisfies(member -> {
            assertThat(member.role()).isEqualTo(Role.SUPERVISOR);
            assertThat(member.status()).isEqualTo(TeamMemberStatus.WAITING_FOR_VERIFICATION);
        });
    }

    @Test
    void aPersonIsOnlyStartedForAVerifiedBusinessInAKnownRole() {
        world.registeredBusiness("business-2");

        assertThatThrownBy(() -> starter.start(new StartTeamMemberRequest("business-2", "HELPER", PHOTO)))
                .isInstanceOf(BusinessNotVerifiedException.class);
        assertThatThrownBy(() -> starter.start(new StartTeamMemberRequest(BUSINESS, "DRIVER", PHOTO)))
                .isInstanceOf(UnknownRoleException.class);
        assertThatThrownBy(() -> starter.start(new StartTeamMemberRequest(BUSINESS, null, PHOTO)))
                .isInstanceOf(UnknownRoleException.class);
        assertThat(world.members.all()).isEmpty();
        assertThat(world.kycCalls).isEmpty();
    }

    @Test
    void aPassedCheckLeavesThePersonWaitingForTheProvidersConfirmation() {
        String memberId = world.startedPerson(BUSINESS, "HELPER");

        String status = results.report(
                new ReportTeamMemberResultRequest(memberId, "PASSED", "Siti binti Ahmad", "MY", NEXT_YEAR)).memberStatus();

        assertThat(status).isEqualTo("WAITING_FOR_CONFIRMATION");
        assertThat(world.members.timesSaved(memberId)).isEqualTo(2);
        assertThat(world.members.find(memberId).orElseThrow().registeredName()).isEqualTo("Siti binti Ahmad");
        assertThat(world.publisher.published(TeamMemberJoinedIE.class)).isEmpty();
    }

    @Test
    void aFailedCheckAddsNothingAndTheProviderIsToldWhich() {
        String memberId = world.startedPerson(BUSINESS, "HELPER");

        String status = results.report(new ReportTeamMemberResultRequest(memberId, "UNDER_18", null, null, null))
                .memberStatus();

        assertThat(status).isEqualTo("CHECK_FAILED");
        assertThat(world.members.timesSaved(memberId)).isEqualTo(2);
        assertThat(world.publisher.published(TeamMemberCheckFailedIE.class))
                .containsExactly(new TeamMemberCheckFailedIE(memberId, BUSINESS, "UNDER_18", NOW));
    }

    @Test
    void anUnknownResultFromTheKycProviderIsRefused() {
        String memberId = world.startedPerson(BUSINESS, "HELPER");

        assertThatThrownBy(() -> results.report(new ReportTeamMemberResultRequest(memberId, "MAYBE", null, null, null)))
                .isInstanceOf(UnknownCheckFailureException.class);
        assertThatThrownBy(() -> results.report(new ReportTeamMemberResultRequest(memberId, null, null, null, null)))
                .isInstanceOf(UnknownCheckFailureException.class);
        assertThatThrownBy(() -> results.report(new ReportTeamMemberResultRequest("none", "PASSED", null, null, null)))
                .isInstanceOf(TeamMemberNotFoundException.class);
    }

    @Test
    void theConfirmedPersonJoinsTheTeamAndTheOtherContextsAreTold() {
        String memberId = world.verifiedPerson(BUSINESS, "HELPER");

        ConfirmTeamMemberResponse response =
                confirmer.confirm(new ConfirmTeamMemberRequest(BUSINESS, memberId, true, false, "terms-v2"));

        assertThat(response).isEqualTo(new ConfirmTeamMemberResponse(memberId, "Siti binti Ahmad", "HELPER"));
        assertThat(world.members.timesSaved(memberId)).isEqualTo(3);
        assertThat(world.members.find(memberId).orElseThrow().confirmation()).get()
                .satisfies(confirmation -> assertThat(confirmation.termsVersion()).isEqualTo("terms-v2"));
        assertThat(world.publisher.published(TeamMemberJoinedIE.class)).containsExactly(
                new TeamMemberJoinedIE(memberId, BUSINESS, "HELPER", "Siti binti Ahmad", PHOTO, NOW, NOW));
    }

    @Test
    void withoutTheTickNobodyJoinsAndNobodyIsTold() {
        String memberId = world.verifiedPerson(BUSINESS, "HELPER");

        assertThatThrownBy(() -> confirmer.confirm(new ConfirmTeamMemberRequest(BUSINESS, memberId, false, false, "terms-v2")))
                .isInstanceOf(EmploymentNotConfirmedException.class);
        assertThat(world.members.timesSaved(memberId)).isEqualTo(2);
        assertThat(world.publisher.published(TeamMemberJoinedIE.class)).isEmpty();
    }

    @Test
    void aProviderOnlyReachesTheirOwnPeople() {
        world.verifiedBusiness("business-2");
        String theirs = world.verifiedPerson("business-2", "HELPER");

        assertThatThrownBy(() -> confirmer.confirm(new ConfirmTeamMemberRequest(BUSINESS, theirs, true, false, "terms-v2")))
                .isInstanceOf(TeamMemberNotFoundException.class);
        assertThatThrownBy(() -> confirmer.confirm(new ConfirmTeamMemberRequest(BUSINESS, "none", true, false, "terms-v2")))
                .isInstanceOf(TeamMemberNotFoundException.class);
    }

    @Test
    void aRoleChangeIsSavedAndAnnouncedOnlyWhenTheRoleChanges() {
        String memberId = world.teamMember(BUSINESS, "HELPER");
        ProviderChangeTeamMemberRole usecase =
                new ProviderChangeTeamMemberRole(world.members, world.publisher, world.clock);

        assertThat(usecase.change(new ChangeTeamMemberRoleRequest(BUSINESS, memberId, "SUPERVISOR")).role())
                .isEqualTo("SUPERVISOR");
        assertThat(usecase.change(new ChangeTeamMemberRoleRequest(BUSINESS, memberId, "SUPERVISOR")).role())
                .isEqualTo("SUPERVISOR");

        assertThat(world.members.timesSaved(memberId)).isEqualTo(4);
        assertThat(world.publisher.published(TeamMemberRoleChangedIE.class))
                .containsExactly(new TeamMemberRoleChangedIE(memberId, BUSINESS, "SUPERVISOR", NOW));
    }

    @Test
    void aRemovedTeamMemberLeavesAndTheOtherContextsAreTold() {
        String memberId = world.teamMember(BUSINESS, "HELPER");

        assertThat(new ProviderRemoveTeamMember(world.members, world.publisher, world.clock)
                .remove(new RemoveTeamMemberRequest(BUSINESS, memberId)).memberId()).isEqualTo(memberId);

        assertThat(world.members.find(memberId).orElseThrow().status()).isEqualTo(TeamMemberStatus.REMOVED);
        assertThat(world.members.timesSaved(memberId)).isEqualTo(4);
        assertThat(world.publisher.published(TeamMemberLeftIE.class))
                .containsExactly(new TeamMemberLeftIE(memberId, BUSINESS, "REMOVED", NOW));
    }

    @Test
    void removingSomeoneStillWaitingTellsNoOne() {
        String memberId = world.verifiedPerson(BUSINESS, "HELPER");

        new ProviderRemoveTeamMember(world.members, world.publisher, world.clock)
                .remove(new RemoveTeamMemberRequest(BUSINESS, memberId));

        assertThat(world.members.find(memberId).orElseThrow().status()).isEqualTo(TeamMemberStatus.REMOVED);
        assertThat(world.members.timesSaved(memberId)).isEqualTo(3);
        assertThat(world.publisher.published(TeamMemberLeftIE.class)).isEmpty();
    }

    @Test
    void homaRemovesATeamMemberWithARecordedReason() {
        String memberId = world.teamMember(BUSINESS, "HELPER");
        String waiting = world.verifiedPerson(BUSINESS, "HELPER");
        AdminRemoveTeamMember usecase = new AdminRemoveTeamMember(world.members, world.publisher, world.clock);

        assertThat(usecase.remove(new RemoveTeamMemberByHomaRequest("admin-1", memberId, "Not allowed to work"))
                .memberId()).isEqualTo(memberId);
        usecase.remove(new RemoveTeamMemberByHomaRequest("admin-1", waiting, "Not allowed to work"));

        assertThat(world.members.find(memberId).orElseThrow().removal()).get().satisfies(removal -> {
            assertThat(removal.removedBy()).isEqualTo("admin-1");
            assertThat(removal.reason()).isEqualTo("Not allowed to work");
        });
        assertThat(world.members.timesSaved(memberId)).isEqualTo(4);
        assertThat(world.members.timesSaved(waiting)).isEqualTo(3);
        assertThat(world.publisher.published(TeamMemberLeftIE.class))
                .containsExactly(new TeamMemberLeftIE(memberId, BUSINESS, "REMOVED", NOW));
    }

    @Test
    void aTeamMemberWhoLapsesLeavesTheTeamOnce() {
        String memberId = world.teamMember(BUSINESS, "HELPER");
        String flagged = world.teamMember(BUSINESS, "HELPER");
        KycReportTeamMemberLapse usecase = new KycReportTeamMemberLapse(world.members, world.publisher, world.clock);

        assertThat(usecase.report(new ReportTeamMemberLapseRequest(memberId, false)).memberStatus()).isEqualTo("LAPSED");
        usecase.report(new ReportTeamMemberLapseRequest(memberId, false));
        usecase.report(new ReportTeamMemberLapseRequest(flagged, true));

        assertThat(world.members.timesSaved(memberId)).isEqualTo(4);
        assertThat(world.publisher.published(TeamMemberLeftIE.class)).containsExactly(
                new TeamMemberLeftIE(memberId, BUSINESS, "LAPSED", NOW),
                new TeamMemberLeftIE(flagged, BUSINESS, "SERIOUS_FLAG", NOW));
    }

    @Test
    void aLapsedTeamMemberWhoPassesIsBackOnTheTeam() {
        String memberId = world.teamMember(BUSINESS, "SUPERVISOR");
        new KycReportTeamMemberLapse(world.members, world.publisher, world.clock)
                .report(new ReportTeamMemberLapseRequest(memberId, false));
        Instant twoYears = NEXT_YEAR.plus(Duration.ofDays(365));

        String status = results.report(new ReportTeamMemberResultRequest(memberId, "PASSED", null, null, twoYears))
                .memberStatus();

        assertThat(status).isEqualTo("ON_TEAM");
        assertThat(world.members.timesSaved(memberId)).isEqualTo(5);
        assertThat(world.members.find(memberId).orElseThrow().verifiedUntil()).isEqualTo(twoYears);
        assertThat(world.publisher.published(TeamMemberRestoredIE.class)).containsExactly(
                new TeamMemberRestoredIE(memberId, BUSINESS, "SUPERVISOR", "Siti binti Ahmad", PHOTO, NOW, NOW));
        assertThat(world.publisher.published(TeamMemberReverifiedIE.class)).isEmpty();
    }

    @Test
    void aReverificationOfSomeoneOnTheTeamMovesTheirDateAndAFailedOneChangesNothing() {
        String memberId = world.teamMember(BUSINESS, "HELPER");
        Instant twoYears = NEXT_YEAR.plus(Duration.ofDays(365));

        results.report(new ReportTeamMemberResultRequest(memberId, "REJECTED", null, null, null));
        assertThat(world.members.timesSaved(memberId)).isEqualTo(3);
        assertThat(world.publisher.published(TeamMemberCheckFailedIE.class)).isEmpty();
        assertThat(world.members.find(memberId).orElseThrow().isOnTeam()).isTrue();

        results.report(new ReportTeamMemberResultRequest(memberId, "PASSED", null, null, twoYears));
        assertThat(world.members.timesSaved(memberId)).isEqualTo(4);
        assertThat(world.publisher.published(TeamMemberReverifiedIE.class))
                .containsExactly(new TeamMemberReverifiedIE(memberId, BUSINESS, NOW, NOW));
        assertThat(world.publisher.published(TeamMemberRestoredIE.class)).isEmpty();
    }

    @Test
    void reverificationNeededSoonerMovesATeamMembersDate() {
        String memberId = world.teamMember(BUSINESS, "HELPER");
        Instant documentExpiry = NOW.plus(Duration.ofDays(30));
        KycReportTeamMemberReverificationNeeded usecase = new KycReportTeamMemberReverificationNeeded(world.members);

        assertThat(usecase.report(new ReportTeamMemberReverificationNeededRequest(memberId, documentExpiry))
                .verifiedUntil()).isEqualTo(documentExpiry);
        assertThat(usecase.report(new ReportTeamMemberReverificationNeededRequest(memberId, NEXT_YEAR))
                .verifiedUntil()).isEqualTo(documentExpiry);
        assertThat(world.members.timesSaved(memberId)).isEqualTo(4);
    }

    @Test
    void theProviderStartsATeamMembersReverificationOnTheSpot() {
        String memberId = world.teamMember(BUSINESS, "HELPER");
        world.kycCalls.clear();

        String session = new ProviderStartTeamMemberReverification(world.members, world.kyc)
                .start(new StartTeamMemberReverificationRequest(BUSINESS, memberId)).session();

        assertThat(session).isEqualTo("session-for-" + memberId);
        assertThat(world.kycCalls).containsExactly("member-check " + memberId + " " + PHOTO);
    }

    @Test
    void theProviderSeesWhenTheBusinessAndEachPersonAreDue() {
        String helper = world.teamMember(BUSINESS, "HELPER");
        String waiting = world.startedPerson(BUSINESS, "SUPERVISOR");
        String removed = world.teamMember(BUSINESS, "HELPER");
        new ProviderRemoveTeamMember(world.members, world.publisher, world.clock)
                .remove(new RemoveTeamMemberRequest(BUSINESS, removed));
        String failed = world.startedPerson(BUSINESS, "HELPER");
        results.report(new ReportTeamMemberResultRequest(failed, "TIMED_OUT", null, null, null));
        world.verifiedBusiness("business-2");
        world.teamMember("business-2", "HELPER");

        ViewVerificationResponse response = new ProviderViewVerification(world.businesses, world.members)
                .view(new ViewVerificationRequest(BUSINESS));

        assertThat(response.businessStatus()).isEqualTo("VERIFIED");
        assertThat(response.registeredName()).isEqualTo("Bersih Sdn Bhd");
        assertThat(response.verifiedUntil()).isEqualTo(NEXT_YEAR);
        assertThat(response.lastVerifiedAt()).isEqualTo(NOW);
        assertThat(response.people()).containsExactly(
                new Person(helper, "ON_TEAM", "HELPER", "Siti binti Ahmad", NEXT_YEAR, NOW),
                new Person(waiting, "WAITING_FOR_VERIFICATION", "SUPERVISOR", null, null, null));
    }
}
