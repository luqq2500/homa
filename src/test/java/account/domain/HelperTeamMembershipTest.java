package account.domain;

import static account.domain.AccountBuilders.aHelperNamed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.AccountDetailRequiredException;
import account.domain.exception.EkycResultNotFinalException;
import account.domain.exception.HelperAlreadyTeamMemberException;
import account.domain.exception.HelperPhotoRequiredException;
import account.domain.exception.HelperRemovedException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Feature: Team membership. */
class HelperTeamMembershipTest {

    private static Helper ainaWhoIs(TeamStatus status) {
        Helper aina = aHelperNamed("Aina");
        switch (status) {
            case INVITE_REJECTED -> aina.recordEkycResult(VerificationStatus.REJECTED);
            case MEMBER -> aina.recordEkycResult(VerificationStatus.VERIFIED);
            case REMOVED -> aina.remove();
            default -> { }
        }
        return aina;
    }

    @Nested
    class Inviting {

        @Test
        void anInvitedHelperIsNotYetATeamMember() {
            Helper aina = Helper.invite("provider-1", " Aina ", Photo.of("photos/aina.jpg"));

            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.INVITED);
            assertThat(aina.isTeamMember()).isFalse();
            assertThat(aina.name()).isEqualTo("Aina");
            assertThat(aina.photo()).isEqualTo(Photo.of("photos/aina.jpg"));
            assertThat(aina.providerId()).isEqualTo("provider-1");
            assertThat(aina.id()).isNotBlank();
        }

        @Test
        void anInviteNeedsANameAndAPhoto() {
            assertThatThrownBy(() -> Helper.invite("provider-1", "Aina", null))
                    .isInstanceOf(HelperPhotoRequiredException.class);
            assertThatThrownBy(() -> Helper.invite("provider-1", " ", Photo.of("photos/aina.jpg")))
                    .isInstanceOf(AccountDetailRequiredException.class)
                    .hasMessage("A helper's name is required");
        }

        @Test
        void anInviteBelongsToAProvider() {
            assertThatThrownBy(() -> Helper.invite(null, "Aina", Photo.of("photos/aina.jpg")))
                    .isInstanceOf(AccountDetailRequiredException.class)
                    .hasMessage("A helper's provider is required");
        }
    }

    @Nested
    class Verifying {

        @Test
        void aVerifiedInviteJoinsTheTeam() {
            Helper aina = ainaWhoIs(TeamStatus.INVITED);

            aina.recordEkycResult(VerificationStatus.VERIFIED);

            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.MEMBER);
            assertThat(aina.isTeamMember()).isTrue();
        }

        @Test
        void aRejectedInviteStaysOffTheTeamAndCanRetry() {
            Helper aina = ainaWhoIs(TeamStatus.INVITED);

            aina.recordEkycResult(VerificationStatus.REJECTED);

            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.INVITE_REJECTED);
            assertThat(aina.isTeamMember()).isFalse();

            aina.recordEkycResult(VerificationStatus.VERIFIED);

            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.MEMBER);
            assertThat(aina.isTeamMember()).isTrue();
        }

        @Test
        void aRejectedInviteCanBeRejectedAgain() {
            Helper aina = ainaWhoIs(TeamStatus.INVITE_REJECTED);

            aina.recordEkycResult(VerificationStatus.REJECTED);

            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.INVITE_REJECTED);
        }

        @Test
        void aTeamMemberIsNotVerifiedAgain() {
            Helper aina = ainaWhoIs(TeamStatus.MEMBER);

            assertThatThrownBy(() -> aina.recordEkycResult(VerificationStatus.REJECTED))
                    .isInstanceOf(HelperAlreadyTeamMemberException.class)
                    .hasMessageContaining(aina.id());
            assertThat(aina.isTeamMember()).isTrue();
        }

        @Test
        void pendingIsNotAnEkycResult() {
            Helper aina = ainaWhoIs(TeamStatus.INVITED);

            assertThatThrownBy(() -> aina.recordEkycResult(VerificationStatus.PENDING))
                    .isInstanceOf(EkycResultNotFinalException.class);
            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.INVITED);
        }
    }

    @Nested
    class Removing {

        @ParameterizedTest(name = "a helper who is {0} can be removed")
        @EnumSource(value = TeamStatus.class, names = {"INVITED", "INVITE_REJECTED", "MEMBER"})
        void theProviderCanWithdrawAnInviteOrRemoveAMember(TeamStatus status) {
            Helper aina = ainaWhoIs(status);

            aina.remove();

            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.REMOVED);
            assertThat(aina.isTeamMember()).isFalse();
        }

        @Test
        void aRemovedHelperCannotBeVerifiedBackOntoTheTeam() {
            Helper aina = ainaWhoIs(TeamStatus.REMOVED);

            assertThatThrownBy(() -> aina.recordEkycResult(VerificationStatus.VERIFIED))
                    .isInstanceOf(HelperRemovedException.class)
                    .hasMessageContaining(aina.id());
            assertThat(aina.isTeamMember()).isFalse();
        }

        @Test
        void aRemovedHelperIsRemovedOnce() {
            Helper aina = ainaWhoIs(TeamStatus.REMOVED);

            assertThatThrownBy(aina::remove).isInstanceOf(HelperRemovedException.class);
        }
    }
}
