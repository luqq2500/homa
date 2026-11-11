package account.domain;

import static account.domain.AccountBuilders.aina;
import static account.domain.AccountBuilders.on;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.NotATeamMemberException;
import account.domain.exception.TeamMemberRemovedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Feature: The KYC provider re-verifies a team member. Aina was verified on 1 January, until 1 April. */
class TeamMemberReverificationTest {

    private TeamMember member;

    @BeforeEach
    void ainaIsVerifiedUntilTheFirstOfApril() {
        member = aina();
    }

    @Test
    void aPassedReverificationKeepsTheMemberVerifiedUntilTheNewDate() {
        VerificationOutcome outcome = member.recordKycResult(KycResult.verified("Aina binti Ahmad", on("20 June")), on("20 March"));

        assertThat(outcome).isEqualTo(VerificationOutcome.REVERIFIED);
        assertThat(member.verifiedUntil()).isEqualTo(on("20 June"));
        assertThat(member.lastVerifiedAt()).isEqualTo(on("20 March"));
        assertThat(member.isTeamMember()).isTrue();
    }

    @Test
    void theRegisteredNameFollowsTheLatestResult() {
        member.recordKycResult(KycResult.verified("Aina binti Ahmad Zaki", on("20 June")), on("20 March"));

        assertThat(member.registeredName()).isEqualTo("Aina binti Ahmad Zaki");
    }

    @Test
    void aRepeatedPassChangesNothing() {
        VerificationOutcome outcome = member.recordKycResult(KycResult.verified("Aina binti Ahmad", on("1 April")), on("2 January"));

        assertThat(outcome).isEqualTo(VerificationOutcome.UNCHANGED);
        assertThat(member.lastVerifiedAt()).isEqualTo(on("1 January"));
    }

    @Test
    void aFailedReverificationChangesNothing() {
        VerificationOutcome outcome = member.recordKycResult(KycResult.rejected(), on("20 March"));

        assertThat(outcome).isEqualTo(VerificationOutcome.RECHECK_FAILED);
        assertThat(member.isTeamMember()).isTrue();
        assertThat(member.verifiedUntil()).isEqualTo(on("1 April"));
    }

    @Test
    void aLapsedMemberLeavesTheTeam() {
        member.lapse();

        assertThat(member.teamStatus()).isEqualTo(TeamStatus.LAPSED);
        assertThat(member.isTeamMember()).isFalse();
    }

    @Test
    void aLapsedMemberWhoPassesRejoinsTheTeam() {
        member.lapse();

        VerificationOutcome outcome = member.recordKycResult(KycResult.verified("Aina binti Ahmad", on("1 April")), on("5 April"));

        assertThat(outcome).isEqualTo(VerificationOutcome.REVERIFIED);
        assertThat(member.teamStatus()).isEqualTo(TeamStatus.MEMBER);
        assertThat(member.lastVerifiedAt()).isEqualTo(on("5 April"));
    }

    @Test
    void aLapsedMemberWhoFailsStaysLapsed() {
        member.lapse();

        assertThat(member.recordKycResult(KycResult.rejected(), on("5 April"))).isEqualTo(VerificationOutcome.RECHECK_FAILED);
        assertThat(member.teamStatus()).isEqualTo(TeamStatus.LAPSED);
    }

    @Test
    void aLapsedMemberDoesNotLapseAgain() {
        member.lapse();

        assertThatThrownBy(member::lapse)
                .isInstanceOf(NotATeamMemberException.class)
                .hasMessageContaining(member.id())
                .hasMessageContaining("LAPSED");
    }

    @Test
    void aRemovedMemberIsNotReverifiedOrLapsed() {
        member.remove();

        assertThatThrownBy(() -> member.recordKycResult(KycResult.verified("Aina binti Ahmad", on("20 June")), on("20 March")))
                .isInstanceOf(TeamMemberRemovedException.class);
        assertThatThrownBy(member::lapse).isInstanceOf(TeamMemberRemovedException.class);
        assertThat(member.teamStatus()).isEqualTo(TeamStatus.REMOVED);
    }
}
