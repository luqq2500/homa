package account.domain;

import static account.domain.AccountBuilders.aHelperWaitingSince;
import static account.domain.AccountBuilders.aProvider;
import static account.domain.AccountBuilders.aVerifiedProvider;
import static account.domain.AccountBuilders.aina;
import static account.domain.AccountBuilders.ainaVerifiedUntil;
import static account.domain.AccountBuilders.confirmedOn;
import static account.domain.AccountBuilders.on;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.AccountDetailRequiredException;
import account.domain.exception.EmploymentNotConfirmedException;
import account.domain.exception.MemberAlreadyAddedException;
import account.domain.exception.MemberNotVerifiedException;
import account.domain.exception.PhotoRequiredException;
import account.domain.exception.ProviderNotVerifiedException;
import account.domain.exception.TeamMemberRemovedException;
import account.domain.exception.VerificationDetailsRequiredException;
import account.domain.exception.VerificationNotWaitingException;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Feature: Adding a team member, verified by the KYC provider. */
class TeamMemberAddingTest {

    @Nested
    class TheAddForm {

        @Test
        void aConfirmedAndVerifiedPersonIsAdded() {
            ProviderAccount kemas = aVerifiedProvider();
            MemberVerification verification = aHelperWaitingSince(kemas, "1 March");

            VerificationOutcome outcome = verification.recordKycResult(ainaVerifiedUntil("1 June"), on("2 March"));
            TeamMember aina = TeamMember.add(verification, on("2 March"));

            assertThat(outcome).isEqualTo(VerificationOutcome.VERIFIED);
            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.MEMBER);
            assertThat(aina.isTeamMember()).isTrue();
            assertThat(aina.role()).isEqualTo(TeamRole.HELPER);
            assertThat(aina.registeredName()).isEqualTo("Aina binti Ahmad");
            assertThat(aina.verifiedUntil()).isEqualTo(on("1 June"));
            assertThat(aina.lastVerifiedAt()).isEqualTo(on("2 March"));
            assertThat(aina.employmentConfirmation()).isEqualTo(new EmploymentConfirmation(on("1 March"), "2026-11"));
            assertThat(aina.photo()).isEqualTo(Photo.of("photos/aina.jpg"));
            assertThat(aina.providerId()).isEqualTo(kemas.id());
            assertThat(aina.addedAt()).isEqualTo(on("2 March"));
            assertThat(aina.id()).isNotBlank().isNotEqualTo(verification.id());
            assertThat(verification.status()).isEqualTo(MemberVerificationStatus.ADDED);
            assertThat(verification.hasEnded()).isFalse();
        }

        @Test
        void aWaitingVerificationIsNotOnTheTeam() {
            ProviderAccount kemas = aVerifiedProvider();
            MemberVerification verification = MemberVerification.start(
                    kemas, TeamRole.SUPERVISOR, Photo.of("photos/siti.jpg"), confirmedOn("1 March"), on("1 March"));

            assertThat(verification.status()).isEqualTo(MemberVerificationStatus.WAITING);
            assertThat(verification.hasEnded()).isFalse();
            assertThat(verification.registeredName()).isEmpty();
            assertThat(verification.verifiedUntil()).isEmpty();
            assertThat(verification.verifiedAt()).isEmpty();
            assertThat(verification.providerId()).isEqualTo(kemas.id());
            assertThat(verification.role()).isEqualTo(TeamRole.SUPERVISOR);
            assertThat(verification.photo()).isEqualTo(Photo.of("photos/siti.jpg"));
            assertThat(verification.confirmation()).isEqualTo(confirmedOn("1 March"));
            assertThat(verification.startedAt()).isEqualTo(on("1 March"));
            assertThat(verification.id()).isNotBlank();
        }

        @Test
        void theAddFormNeedsTheConfirmation() {
            assertThatThrownBy(() -> MemberVerification.start(
                            aVerifiedProvider(), TeamRole.HELPER, Photo.of("photos/aina.jpg"), null, on("1 March")))
                    .isInstanceOf(EmploymentNotConfirmedException.class);
        }

        @Test
        void theAddFormNeedsAProviderARolePhotoAndTime() {
            ProviderAccount kemas = aVerifiedProvider();
            assertThatThrownBy(() -> MemberVerification.start(kemas, TeamRole.HELPER, null, confirmedOn("1 March"), on("1 March")))
                    .isInstanceOf(PhotoRequiredException.class);
            assertThatThrownBy(() -> MemberVerification.start(null, TeamRole.HELPER, Photo.of("photos/aina.jpg"), confirmedOn("1 March"), on("1 March")))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> MemberVerification.start(kemas, null, Photo.of("photos/aina.jpg"), confirmedOn("1 March"), on("1 March")))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> MemberVerification.start(kemas, TeamRole.HELPER, Photo.of("photos/aina.jpg"), confirmedOn("1 March"), null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void aBusinessWhoseKybIsPendingCannotAddTeamMembers() {
            ProviderAccount waiting = aProvider();

            assertThatThrownBy(() -> aHelperWaitingSince(waiting, "1 March"))
                    .isInstanceOf(ProviderNotVerifiedException.class)
                    .hasMessageContaining(waiting.id())
                    .hasMessageContaining("PENDING");
        }

        @Test
        void aBusinessWhoseKybWasRejectedCannotAddTeamMembers() {
            ProviderAccount rejected = aProvider();
            rejected.recordKybResult(KybResult.rejected(), on("1 January"));

            assertThatThrownBy(() -> aHelperWaitingSince(rejected, "1 March"))
                    .isInstanceOf(ProviderNotVerifiedException.class)
                    .hasMessageContaining("REJECTED");
        }

        @Test
        void aLapsedBusinessCannotAddTeamMembers() {
            ProviderAccount lapsed = aVerifiedProvider();
            lapsed.lapse();

            assertThatThrownBy(() -> aHelperWaitingSince(lapsed, "1 March"))
                    .isInstanceOf(ProviderNotVerifiedException.class)
                    .hasMessageContaining("LAPSED");
        }

        @Test
        void theConfirmationRecordsItsTimeAndTermsVersion() {
            EmploymentConfirmation confirmation = EmploymentConfirmation.given(" 2026-11 ", on("1 March"));

            assertThat(confirmation.termsVersion()).isEqualTo("2026-11");
            assertThat(confirmation.givenAt()).isEqualTo(on("1 March"));
            assertThatThrownBy(() -> EmploymentConfirmation.given(" ", on("1 March")))
                    .isInstanceOf(AccountDetailRequiredException.class);
            assertThatThrownBy(() -> EmploymentConfirmation.given("2026-11", null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class TheKycResult {

        @Test
        void aPersonCannotBeAddedBeforeVerification() {
            MemberVerification verification = aHelperWaitingSince("1 March");

            assertThatThrownBy(() -> TeamMember.add(verification, on("2 March")))
                    .isInstanceOf(MemberNotVerifiedException.class)
                    .hasMessageContaining(verification.id())
                    .hasMessageContaining("WAITING");
            assertThat(verification.status()).isEqualTo(MemberVerificationStatus.WAITING);
        }

        @Test
        void aRejectedVerificationAddsNothing() {
            MemberVerification verification = aHelperWaitingSince("1 March");

            VerificationOutcome outcome = verification.recordKycResult(KycResult.rejected(), on("2 March"));

            assertThat(outcome).isEqualTo(VerificationOutcome.REJECTED);
            assertThat(verification.status()).isEqualTo(MemberVerificationStatus.REJECTED);
            assertThat(verification.hasEnded()).isTrue();
            assertThatThrownBy(() -> TeamMember.add(verification, on("2 March"))).isInstanceOf(MemberNotVerifiedException.class);
        }

        @Test
        void aVerificationIsUsedOnce() {
            MemberVerification verification = aHelperWaitingSince("1 March");
            verification.recordKycResult(ainaVerifiedUntil("1 June"), on("2 March"));
            TeamMember.add(verification, on("2 March"));

            assertThatThrownBy(() -> TeamMember.add(verification, on("3 March")))
                    .isInstanceOf(MemberAlreadyAddedException.class)
                    .hasMessageContaining(verification.id());
        }

        @Test
        void aRepeatedResultChangesNothing() {
            MemberVerification verified = aHelperWaitingSince("1 March");
            verified.recordKycResult(ainaVerifiedUntil("1 June"), on("2 March"));
            MemberVerification rejected = aHelperWaitingSince("1 March");
            rejected.recordKycResult(KycResult.rejected(), on("2 March"));

            assertThat(verified.recordKycResult(ainaVerifiedUntil("1 June"), on("3 March"))).isEqualTo(VerificationOutcome.UNCHANGED);
            assertThat(verified.verifiedAt()).contains(on("2 March"));
            assertThat(rejected.recordKycResult(KycResult.rejected(), on("3 March"))).isEqualTo(VerificationOutcome.UNCHANGED);

            TeamMember.add(verified, on("3 March"));
            assertThat(verified.recordKycResult(ainaVerifiedUntil("1 June"), on("4 March"))).isEqualTo(VerificationOutcome.UNCHANGED);
        }

        @Test
        void anEndedOrVerifiedVerificationTakesNoDifferentResult() {
            MemberVerification verified = aHelperWaitingSince("1 March");
            verified.recordKycResult(ainaVerifiedUntil("1 June"), on("2 March"));
            MemberVerification rejected = aHelperWaitingSince("1 March");
            rejected.recordKycResult(KycResult.rejected(), on("2 March"));

            assertThatThrownBy(() -> verified.recordKycResult(KycResult.rejected(), on("3 March")))
                    .isInstanceOf(VerificationNotWaitingException.class)
                    .hasMessageContaining("VERIFIED");
            assertThatThrownBy(() -> rejected.recordKycResult(ainaVerifiedUntil("1 June"), on("3 March")))
                    .isInstanceOf(VerificationNotWaitingException.class)
                    .hasMessageContaining("REJECTED");
        }

        @Test
        void aVerifiedResultCarriesTheRegisteredNameAndTheDate() {
            assertThatThrownBy(() -> KycResult.verified(null, on("1 June")))
                    .isInstanceOf(VerificationDetailsRequiredException.class)
                    .hasMessageContaining("registered name");
            assertThatThrownBy(() -> KycResult.verified(" ", on("1 June")))
                    .isInstanceOf(AccountDetailRequiredException.class);
            assertThatThrownBy(() -> KycResult.verified("Aina binti Ahmad", null))
                    .isInstanceOf(VerificationDetailsRequiredException.class)
                    .hasMessageContaining("verified-until");
            assertThatThrownBy(() -> new KycResult(VerificationStatus.REJECTED, null, Optional.empty()))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new KycResult(VerificationStatus.REJECTED, Optional.empty(), null))
                    .isInstanceOf(NullPointerException.class);
            assertThat(KycResult.verified(" Aina binti Ahmad ", on("1 June")).registeredName()).contains("Aina binti Ahmad");
            assertThat(KycResult.rejected().isVerified()).isFalse();
        }
    }

    @Nested
    class Removing {

        @ParameterizedTest(name = "a {0} member can be removed")
        @EnumSource(value = TeamStatus.class, names = {"MEMBER", "LAPSED"})
        void theProviderCanRemoveATeamMember(TeamStatus status) {
            TeamMember aina = aina();
            if (status == TeamStatus.LAPSED) {
                aina.lapse();
            }

            aina.remove();

            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.REMOVED);
            assertThat(aina.isTeamMember()).isFalse();
        }

        @Test
        void aRemovedMemberIsRemovedOnce() {
            TeamMember aina = aina();
            aina.remove();

            assertThatThrownBy(aina::remove).isInstanceOf(TeamMemberRemovedException.class).hasMessageContaining(aina.id());
        }
    }
}
