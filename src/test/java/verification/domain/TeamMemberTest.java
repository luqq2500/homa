package verification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static verification.support.VerificationFixtures.BUSINESS_ID;
import static verification.support.VerificationFixtures.NEXT_YEAR;
import static verification.support.VerificationFixtures.NOW;
import static verification.support.VerificationFixtures.PHOTO;
import static verification.support.VerificationFixtures.onTeam;
import static verification.support.VerificationFixtures.verifiedBusiness;
import static verification.support.VerificationFixtures.waitingForConfirmation;
import static verification.support.VerificationFixtures.waitingForVerification;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import verification.domain.exception.BusinessNotVerifiedException;
import verification.domain.exception.EmploymentNotConfirmedException;
import verification.domain.exception.ProfilePhotoMissingException;
import verification.domain.exception.ReasonMissingException;
import verification.domain.exception.TeamMemberNotAwaitingConfirmationException;
import verification.domain.exception.TeamMemberNotAwaitingVerificationException;
import verification.domain.exception.TeamMemberNotOnTeamException;
import verification.domain.exception.WorkPassNotConfirmedException;

class TeamMemberTest {

    @Test
    void aStartedPersonWaitsForVerificationAndIsNotOnTheTeam() {
        TeamMember member = TeamMember.start(verifiedBusiness(), Role.SUPERVISOR, PHOTO);

        assertThat(member.id()).isNotBlank().isNotEqualTo(waitingForVerification(Role.HELPER).id());
        assertThat(member.businessId()).isEqualTo(BUSINESS_ID);
        assertThat(member.role()).isEqualTo(Role.SUPERVISOR);
        assertThat(member.profilePhoto()).isEqualTo(PHOTO);
        assertThat(member.status()).isEqualTo(TeamMemberStatus.WAITING_FOR_VERIFICATION);
        assertThat(member.isOnTeam()).isFalse();
    }

    @Test
    void aBusinessAddsPeopleOnlyOnceItsKybHasPassedAndWhileItHasNotLapsed() {
        Business unverified = Business.register(BUSINESS_ID);
        Business lapsed = verifiedBusiness();
        lapsed.lapse();

        assertThatThrownBy(() -> TeamMember.start(unverified, Role.HELPER, PHOTO))
                .isInstanceOf(BusinessNotVerifiedException.class);
        assertThatThrownBy(() -> TeamMember.start(lapsed, Role.HELPER, PHOTO))
                .isInstanceOf(BusinessNotVerifiedException.class);
    }

    @Test
    void aPersonIsAddedWithAProfilePhoto() {
        assertThatThrownBy(() -> TeamMember.start(verifiedBusiness(), Role.HELPER, " "))
                .isInstanceOf(ProfilePhotoMissingException.class);
        assertThatThrownBy(() -> TeamMember.start(verifiedBusiness(), Role.HELPER, null))
                .isInstanceOf(ProfilePhotoMissingException.class);
    }

    @Test
    void aVerifiedPersonTakesTheNameOnTheirDocumentAndWaitsForConfirmation() {
        TeamMember member = waitingForVerification(Role.HELPER);

        member.verified("Siti binti Ahmad", "MY", NEXT_YEAR, NOW);

        assertThat(member.status()).isEqualTo(TeamMemberStatus.WAITING_FOR_CONFIRMATION);
        assertThat(member.registeredName()).isEqualTo("Siti binti Ahmad");
        assertThat(member.documentCountry()).isEqualTo("MY");
        assertThat(member.verifiedUntil()).isEqualTo(NEXT_YEAR);
        assertThat(member.lastVerifiedAt()).isEqualTo(NOW);
        assertThat(member.isOnTeam()).isFalse();
    }

    @Test
    void aFailedCheckAddsNothingAndRecordsWhy() {
        TeamMember member = waitingForVerification(Role.HELPER);

        member.checkFailed(CheckFailure.UNDER_18);

        assertThat(member.status()).isEqualTo(TeamMemberStatus.CHECK_FAILED);
        assertThat(member.checkFailure()).contains(CheckFailure.UNDER_18);
        assertThat(member.isOnTeam()).isFalse();
    }

    @Test
    void aCheckResultOnlyCountsForAPersonWaitingForVerification() {
        TeamMember member = waitingForConfirmation(Role.HELPER);

        assertThatThrownBy(() -> member.verified("Other", "MY", NEXT_YEAR, NOW))
                .isInstanceOf(TeamMemberNotAwaitingVerificationException.class);
        assertThatThrownBy(() -> member.checkFailed(CheckFailure.REJECTED))
                .isInstanceOf(TeamMemberNotAwaitingVerificationException.class);
        assertThat(member.registeredName()).isEqualTo("Siti binti Ahmad");
        assertThat(member.checkFailure()).isEmpty();
    }

    @Test
    void thePersonJoinsTheTeamOnlyOnceTheProviderConfirmsTheirEmployment() {
        TeamMember member = waitingForConfirmation(Role.HELPER);

        member.confirm(true, false, "terms-v3", NOW.plusSeconds(30));

        assertThat(member.status()).isEqualTo(TeamMemberStatus.ON_TEAM);
        assertThat(member.isOnTeam()).isTrue();
        assertThat(member.confirmation()).contains(new EmploymentConfirmation("terms-v3", false, NOW.plusSeconds(30)));
    }

    @Test
    void withoutTheLawfulEmploymentTickThePersonDoesNotJoin() {
        TeamMember member = waitingForConfirmation(Role.HELPER);

        assertThatThrownBy(() -> member.confirm(false, true, "terms-v3", NOW))
                .isInstanceOf(EmploymentNotConfirmedException.class);
        assertThat(member.status()).isEqualTo(TeamMemberStatus.WAITING_FOR_CONFIRMATION);
        assertThat(member.confirmation()).isEmpty();
    }

    @Test
    void forADocumentFromAnotherCountryTheProviderAlsoConfirmsAValidWorkPass() {
        TeamMember foreign = waitingForVerification(Role.HELPER);
        foreign.verified("Maria Santos", "PH", NEXT_YEAR, NOW);

        assertThatThrownBy(() -> foreign.confirm(true, false, "terms-v3", NOW))
                .isInstanceOf(WorkPassNotConfirmedException.class);
        assertThat(foreign.isOnTeam()).isFalse();

        foreign.confirm(true, true, "terms-v3", NOW);
        assertThat(foreign.isOnTeam()).isTrue();
        assertThat(foreign.confirmation()).contains(new EmploymentConfirmation("terms-v3", true, NOW));
    }

    @Test
    void aPersonIsConfirmedOnlyAfterTheyAreVerified() {
        assertThatThrownBy(() -> waitingForVerification(Role.HELPER).confirm(true, false, "terms-v3", NOW))
                .isInstanceOf(TeamMemberNotAwaitingConfirmationException.class);
        assertThatThrownBy(() -> onTeam(Role.HELPER).confirm(true, false, "terms-v4", NOW))
                .isInstanceOf(TeamMemberNotAwaitingConfirmationException.class);
    }

    @Test
    void aTeamMemberChangesRoleWithoutVerifyingAgain() {
        TeamMember member = onTeam(Role.HELPER);

        assertThat(member.changeRole(Role.SUPERVISOR)).isTrue();

        assertThat(member.role()).isEqualTo(Role.SUPERVISOR);
        assertThat(member.status()).isEqualTo(TeamMemberStatus.ON_TEAM);
        assertThat(member.lastVerifiedAt()).isEqualTo(NOW);
        assertThat(member.changeRole(Role.SUPERVISOR)).isFalse();
    }

    @Test
    void onlySomeoneWhoJoinedTheTeamChangesRole() {
        TeamMember waiting = waitingForConfirmation(Role.HELPER);

        assertThatThrownBy(() -> waiting.changeRole(Role.SUPERVISOR)).isInstanceOf(TeamMemberNotOnTeamException.class);
        assertThat(waiting.role()).isEqualTo(Role.HELPER);

        TeamMember lapsed = onTeam(Role.HELPER);
        lapsed.lapse();
        assertThat(lapsed.changeRole(Role.SUPERVISOR)).isTrue();
    }

    @Test
    void aPassedReverificationMovesTheDateOut() {
        TeamMember member = onTeam(Role.HELPER);
        Instant later = NOW.plus(Duration.ofDays(350));
        Instant twoYears = NEXT_YEAR.plus(Duration.ofDays(365));

        assertThat(member.reverified(twoYears, later)).isFalse();

        assertThat(member.verifiedUntil()).isEqualTo(twoYears);
        assertThat(member.lastVerifiedAt()).isEqualTo(later);
        assertThat(member.isOnTeam()).isTrue();
    }

    @Test
    void aTeamMemberWhoMissesTheirReverificationLeavesTheTeamUntilTheyPass() {
        TeamMember member = onTeam(Role.HELPER);

        assertThat(member.lapse()).isTrue();
        assertThat(member.status()).isEqualTo(TeamMemberStatus.LAPSED);
        assertThat(member.isOnTeam()).isFalse();
        assertThat(member.lapse()).isFalse();

        assertThat(member.reverified(NEXT_YEAR.plusSeconds(1), NOW.plusSeconds(1))).isTrue();
        assertThat(member.isOnTeam()).isTrue();
    }

    @Test
    void someoneNotYetOnTheTeamCannotLapseOrBeReverified() {
        TeamMember waiting = waitingForConfirmation(Role.HELPER);

        assertThat(waiting.lapse()).isFalse();
        assertThat(waiting.status()).isEqualTo(TeamMemberStatus.WAITING_FOR_CONFIRMATION);
        assertThatThrownBy(() -> waiting.reverified(NEXT_YEAR, NOW)).isInstanceOf(TeamMemberNotOnTeamException.class);
        assertThatThrownBy(() -> waiting.reverificationNeededBy(NOW)).isInstanceOf(TeamMemberNotOnTeamException.class);
        assertThat(waiting.verifiedUntil()).isEqualTo(NEXT_YEAR);
    }

    @Test
    void reverificationNeededByASoonerDateBringsVerifiedUntilForward() {
        TeamMember member = onTeam(Role.HELPER);
        Instant documentExpiry = NOW.plus(Duration.ofDays(30));

        assertThat(member.reverificationNeededBy(NEXT_YEAR)).isFalse();
        assertThat(member.reverificationNeededBy(NEXT_YEAR.plusSeconds(1))).isFalse();
        assertThat(member.verifiedUntil()).isEqualTo(NEXT_YEAR);

        assertThat(member.reverificationNeededBy(documentExpiry)).isTrue();
        assertThat(member.verifiedUntil()).isEqualTo(documentExpiry);
        assertThat(member.isOnTeam()).isTrue();
    }

    @Test
    void theProviderCanRemoveAPersonAtAnyTime() {
        TeamMember onTeam = onTeam(Role.HELPER);
        TeamMember waiting = waitingForVerification(Role.HELPER);
        TeamMember lapsed = onTeam(Role.HELPER);
        lapsed.lapse();

        assertThat(onTeam.removeByProvider(NOW)).isTrue();
        assertThat(waiting.removeByProvider(NOW)).isFalse();
        assertThat(lapsed.removeByProvider(NOW)).isFalse();

        assertThat(onTeam.status()).isEqualTo(TeamMemberStatus.REMOVED);
        assertThat(onTeam.isOnTeam()).isFalse();
        assertThat(onTeam.removal()).contains(new Removal(BUSINESS_ID, null, NOW));
        assertThat(waiting.status()).isEqualTo(TeamMemberStatus.REMOVED);
    }

    @Test
    void homaRemovesAPersonWithARecordedReason() {
        TeamMember member = onTeam(Role.HELPER);

        assertThatThrownBy(() -> member.removeByHoma("admin-1", " ", NOW)).isInstanceOf(ReasonMissingException.class);
        assertThatThrownBy(() -> member.removeByHoma("admin-1", null, NOW)).isInstanceOf(ReasonMissingException.class);
        assertThat(member.isOnTeam()).isTrue();

        assertThat(member.removeByHoma("admin-1", "Not allowed to work in Malaysia", NOW)).isTrue();
        assertThat(member.removal()).contains(new Removal("admin-1", "Not allowed to work in Malaysia", NOW));
        assertThat(member.status()).isEqualTo(TeamMemberStatus.REMOVED);
    }

    @Test
    void aRemovedOrFailedPersonCannotBeRemovedOrChanged() {
        TeamMember removed = onTeam(Role.HELPER);
        removed.removeByProvider(NOW);
        TeamMember failed = waitingForVerification(Role.HELPER);
        failed.checkFailed(CheckFailure.TIMED_OUT);

        assertThatThrownBy(() -> removed.removeByProvider(NOW.plusSeconds(1))).isInstanceOf(TeamMemberNotOnTeamException.class);
        assertThatThrownBy(() -> failed.removeByProvider(NOW)).isInstanceOf(TeamMemberNotOnTeamException.class);
        assertThatThrownBy(() -> removed.changeRole(Role.SUPERVISOR)).isInstanceOf(TeamMemberNotOnTeamException.class);
        assertThat(removed.removal()).contains(new Removal(BUSINESS_ID, null, NOW));
        assertThat(failed.removal()).isEmpty();
        assertThat(failed.status()).isEqualTo(TeamMemberStatus.CHECK_FAILED);
    }

    @Test
    void aStoredPersonKeepsEverythingRecordedAboutThem() {
        EmploymentConfirmation confirmation = new EmploymentConfirmation("terms-v1", false, NOW);
        Removal removal = new Removal("admin-1", "Left Malaysia", NOW);

        TeamMember stored = TeamMember.stored("member-1", BUSINESS_ID, PHOTO, Role.SUPERVISOR, TeamMemberStatus.REMOVED,
                "Siti binti Ahmad", "MY", NEXT_YEAR, NOW, CheckFailure.REJECTED, confirmation, removal);

        assertThat(stored.id()).isEqualTo("member-1");
        assertThat(stored.businessId()).isEqualTo(BUSINESS_ID);
        assertThat(stored.profilePhoto()).isEqualTo(PHOTO);
        assertThat(stored.role()).isEqualTo(Role.SUPERVISOR);
        assertThat(stored.status()).isEqualTo(TeamMemberStatus.REMOVED);
        assertThat(stored.registeredName()).isEqualTo("Siti binti Ahmad");
        assertThat(stored.documentCountry()).isEqualTo("MY");
        assertThat(stored.verifiedUntil()).isEqualTo(NEXT_YEAR);
        assertThat(stored.lastVerifiedAt()).isEqualTo(NOW);
        assertThat(stored.checkFailure()).contains(CheckFailure.REJECTED);
        assertThat(stored.confirmation()).contains(confirmation);
        assertThat(stored.removal()).contains(removal);
    }

    @Test
    void anEarlyRecheckIsRequestedWithAReason() {
        EarlyRecheck recheck = EarlyRecheck.request("member-1", "Safety report on reservation r-9", "admin-1", NOW);

        assertThat(recheck.id()).isNotBlank()
                .isNotEqualTo(EarlyRecheck.request("member-1", "Safety report", "admin-1", NOW).id());
        assertThat(recheck.subjectId()).isEqualTo("member-1");
        assertThat(recheck.reason()).isEqualTo("Safety report on reservation r-9");
        assertThat(recheck.adminId()).isEqualTo("admin-1");
        assertThat(recheck.requestedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> EarlyRecheck.request("member-1", " ", "admin-1", NOW))
                .isInstanceOf(ReasonMissingException.class);
        assertThatThrownBy(() -> EarlyRecheck.request("member-1", null, "admin-1", NOW))
                .isInstanceOf(ReasonMissingException.class);
    }
}
