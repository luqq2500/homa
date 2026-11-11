package account.domain;

import static account.domain.AccountBuilders.aProvider;
import static account.domain.AccountBuilders.aVerifiedProvider;
import static account.domain.AccountBuilders.aina;
import static account.domain.AccountBuilders.kemasVerifiedUntil;
import static account.domain.AccountBuilders.on;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.AccountDetailRequiredException;
import account.domain.exception.NotATeamMemberException;
import account.domain.exception.ProviderNotVerifiedException;
import account.domain.exception.TeamMemberRemovedException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Feature: Ongoing monitoring by the KYC provider. Kemas and Aina are verified until 1 April; the KYC provider
 * re-verifies them every year and tells Homa when they need re-verification sooner, or when it finds a
 * serious flag.
 */
class OngoingMonitoringTest {

    @Nested
    class ABusiness {

        @Test
        void aRequiredReverificationMovesTheDateSooner() {
            ProviderAccount kemas = aVerifiedProvider();

            kemas.requireReverification("Director changed", on("15 February"));

            assertThat(kemas.verifiedUntil()).contains(on("15 February"));
            assertThat(kemas.verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        }

        @Test
        void aRequiredReverificationNeverMovesTheDateLater() {
            ProviderAccount kemas = aVerifiedProvider();

            kemas.requireReverification("Address changed", on("1 May"));

            assertThat(kemas.verifiedUntil()).contains(on("1 April"));
        }

        @Test
        void onlyAVerifiedBusinessIsAskedToReverify() {
            ProviderAccount pending = aProvider();

            assertThatThrownBy(() -> pending.requireReverification("Director changed", on("15 February")))
                    .isInstanceOf(ProviderNotVerifiedException.class)
                    .hasMessageContaining("PENDING");
        }

        @ParameterizedTest(name = "the reason \"{0}\" is not enough")
        @NullSource
        @ValueSource(strings = {"", " "})
        void aRequiredReverificationNeedsAReasonAndADeadline(String reason) {
            ProviderAccount kemas = aVerifiedProvider();

            assertThatThrownBy(() -> kemas.requireReverification(reason, on("15 February")))
                    .isInstanceOf(AccountDetailRequiredException.class);
            assertThatThrownBy(() -> kemas.requireReverification("Director changed", null))
                    .isInstanceOf(NullPointerException.class);
            assertThat(kemas.verifiedUntil()).contains(on("1 April"));
        }

        @Test
        void aSeriousFlagLapsesTheBusinessAtOnce() {
            ProviderAccount kemas = aVerifiedProvider();
            kemas.recordPayoutAccountReady();

            kemas.recordSeriousFlag(" Struck off the SSM register ");

            assertThat(kemas.verificationStatus()).isEqualTo(VerificationStatus.LAPSED);
            assertThat(kemas.seriousFlag()).contains("Struck off the SSM register");
            assertThat(kemas.isReadyForGoLive()).isFalse();
        }

        @Test
        void aLapsedBusinessCanStillBeFlagged() {
            ProviderAccount kemas = aVerifiedProvider();
            kemas.lapse();

            kemas.recordSeriousFlag("Sanctions match");

            assertThat(kemas.seriousFlag()).contains("Sanctions match");
        }

        @Test
        void onlyAVerifiedOrLapsedBusinessIsFlagged() {
            ProviderAccount pending = aProvider();

            assertThatThrownBy(() -> pending.recordSeriousFlag("Sanctions match"))
                    .isInstanceOf(ProviderNotVerifiedException.class);
            assertThat(pending.seriousFlag()).isEmpty();
        }

        @Test
        void aFlagNeedsAReason() {
            ProviderAccount kemas = aVerifiedProvider();

            assertThatThrownBy(() -> kemas.recordSeriousFlag(" ")).isInstanceOf(AccountDetailRequiredException.class);
            assertThat(kemas.verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        }

        @Test
        void aPassAfterAFlagClearsIt() {
            ProviderAccount kemas = aVerifiedProvider();
            kemas.recordSeriousFlag("Sanctions match");

            kemas.recordKybResult(kemasVerifiedUntil("1 June"), on("1 March"));

            assertThat(kemas.verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
            assertThat(kemas.seriousFlag()).isEmpty();
        }
    }

    @Nested
    class ATeamMember {

        @Test
        void anExpiringDocumentMovesTheDateSooner() {
            TeamMember member = aina();

            member.requireReverification("Passport expires", on("10 March"));

            assertThat(member.verifiedUntil()).isEqualTo(on("10 March"));
            assertThat(member.isTeamMember()).isTrue();
        }

        @Test
        void aRequiredReverificationNeverMovesTheDateLater() {
            TeamMember member = aina();

            member.requireReverification("Passport expires", on("1 May"));

            assertThat(member.verifiedUntil()).isEqualTo(on("1 April"));
        }

        @Test
        void onlyAMemberOnTheTeamIsAskedToReverify() {
            TeamMember lapsed = aina();
            lapsed.lapse();
            TeamMember removed = aina();
            removed.remove();

            assertThatThrownBy(() -> lapsed.requireReverification("Passport expires", on("10 March")))
                    .isInstanceOf(NotATeamMemberException.class);
            assertThatThrownBy(() -> removed.requireReverification("Passport expires", on("10 March")))
                    .isInstanceOf(TeamMemberRemovedException.class);
        }

        @Test
        void aRequiredReverificationNeedsAReasonAndADeadline() {
            TeamMember member = aina();

            assertThatThrownBy(() -> member.requireReverification(" ", on("10 March")))
                    .isInstanceOf(AccountDetailRequiredException.class);
            assertThatThrownBy(() -> member.requireReverification("Passport expires", null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void aSeriousFlagTakesTheMemberOffTheTeam() {
            TeamMember member = aina();

            member.lapse();

            assertThat(member.isTeamMember()).isFalse();
        }
    }
}
