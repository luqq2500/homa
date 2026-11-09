package account.domain;

import static account.domain.AccountBuilders.ACCEPTED;
import static account.domain.AccountBuilders.INDONESIA;
import static account.domain.AccountBuilders.LIMITS;
import static account.domain.AccountBuilders.aHelperWaitingSince;
import static account.domain.AccountBuilders.aMyKad;
import static account.domain.AccountBuilders.aTeamMemberSince;
import static account.domain.AccountBuilders.confirmedOn;
import static account.domain.AccountBuilders.on;
import static account.domain.AccountBuilders.startAdding;
import static account.domain.AccountBuilders.verifiedAsAina;
import static account.domain.AccountBuilders.verifiedWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.AccountDetailRequiredException;
import account.domain.exception.DocumentNotAcceptedException;
import account.domain.exception.EmploymentNotConfirmedException;
import account.domain.exception.InvalidCountryException;
import account.domain.exception.MemberAlreadyAddedException;
import account.domain.exception.MemberNotVerifiedException;
import account.domain.exception.PhotoMatchAlreadyRecordedException;
import account.domain.exception.PhotoRequiredException;
import account.domain.exception.TeamMemberRemovedException;
import account.domain.exception.VerificationNotExpiredException;
import account.domain.exception.VerificationNotWaitingException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Feature: Adding a team member, and the team member's identity. */
class TeamMemberAddingTest {

    /** The 1-hour session for a verification started at 10:00 on 1 March ends at 11:00. */
    private static final Instant ELEVEN_ON_1_MARCH = on("1 March").plus(Duration.ofHours(1));

    private static final EkycResult INDONESIAN_PASSPORT =
            verifiedWith(new IdentityDocument(INDONESIA, DocumentType.PASSPORT, "Ahmad bin Ismail"));

    @Nested
    class TheAddForm {

        @Test
        void aConfirmedAndVerifiedPersonIsAdded() {
            MemberVerification verification = aHelperWaitingSince("1 March");

            EkycOutcome outcome = verification.recordEkycResult(verifiedAsAina(), ACCEPTED, on("2 March"));
            TeamMember aina = TeamMember.add(verification, on("2 March"));

            assertThat(outcome).isEqualTo(EkycOutcome.VERIFIED);
            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.MEMBER);
            assertThat(aina.isTeamMember()).isTrue();
            assertThat(aina.role()).isEqualTo(TeamRole.HELPER);
            assertThat(aina.registeredName()).isEqualTo("Aina binti Ahmad");
            assertThat(aina.document()).isEqualTo(aMyKad("Aina binti Ahmad"));
            assertThat(aina.employmentConfirmation()).isEqualTo(new EmploymentConfirmation(on("1 March"), "2026-11"));
            assertThat(aina.verification()).isEqualTo(new Verification(on("2 March"), on("2 June"), false, Optional.empty()));
            assertThat(aina.photo()).isEqualTo(Photo.of("photos/aina.jpg"));
            assertThat(aina.providerId()).isEqualTo("provider-1");
            assertThat(aina.providerType()).isEqualTo(ProviderType.INDEPENDENT);
            assertThat(aina.addedAt()).isEqualTo(on("2 March"));
            assertThat(aina.id()).isNotBlank().isNotEqualTo(verification.id());
            assertThat(verification.status()).isEqualTo(MemberVerificationStatus.ADDED);
            assertThat(verification.hasEnded()).isFalse();
        }

        @Test
        void aWaitingVerificationIsNotOnTheTeam() {
            MemberVerification verification =
                    startAdding(ProviderType.BUSINESS, TeamRole.SUPERVISOR, DocumentType.PASSPORT, INDONESIA, "1 March");

            assertThat(verification.status()).isEqualTo(MemberVerificationStatus.WAITING);
            assertThat(verification.hasEnded()).isFalse();
            assertThat(verification.document()).isEmpty();
            assertThat(verification.verification()).isEmpty();
            assertThat(verification.providerId()).isEqualTo("provider-1");
            assertThat(verification.providerType()).isEqualTo(ProviderType.BUSINESS);
            assertThat(verification.role()).isEqualTo(TeamRole.SUPERVISOR);
            assertThat(verification.documentType()).isEqualTo(DocumentType.PASSPORT);
            assertThat(verification.issuingCountry()).isEqualTo(INDONESIA);
            assertThat(verification.photo()).isEqualTo(Photo.of("photos/aina.jpg"));
            assertThat(verification.confirmation()).isEqualTo(confirmedOn("1 March"));
            assertThat(verification.expiresAt()).isEqualTo(on("1 March").plus(Duration.ofHours(1)));
            assertThat(verification.id()).isNotBlank();
        }

        @Test
        void theAddFormNeedsTheConfirmation() {
            assertThatThrownBy(() -> MemberVerification.start(
                            "provider-1", ProviderType.INDEPENDENT, TeamRole.HELPER, DocumentType.IDENTITY_CARD, Country.MALAYSIA,
                            Photo.of("photos/aina.jpg"), null, ACCEPTED, LIMITS, on("1 March")))
                    .isInstanceOf(EmploymentNotConfirmedException.class);
        }

        @Test
        void theAddFormNeedsAProviderARoleAndAPhoto() {
            assertThatThrownBy(() -> MemberVerification.start(
                            "provider-1", ProviderType.INDEPENDENT, TeamRole.HELPER, DocumentType.IDENTITY_CARD, Country.MALAYSIA,
                            null, confirmedOn("1 March"), ACCEPTED, LIMITS, on("1 March")))
                    .isInstanceOf(PhotoRequiredException.class);
            assertThatThrownBy(() -> MemberVerification.start(
                            " ", ProviderType.INDEPENDENT, TeamRole.HELPER, DocumentType.IDENTITY_CARD, Country.MALAYSIA,
                            Photo.of("photos/aina.jpg"), confirmedOn("1 March"), ACCEPTED, LIMITS, on("1 March")))
                    .isInstanceOf(AccountDetailRequiredException.class)
                    .hasMessage("A team member's provider is required");
            assertThatThrownBy(() -> MemberVerification.start(
                            "provider-1", ProviderType.INDEPENDENT, null, DocumentType.IDENTITY_CARD, Country.MALAYSIA,
                            Photo.of("photos/aina.jpg"), confirmedOn("1 March"), ACCEPTED, LIMITS, on("1 March")))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "a {0} provider adding a {1} issued by {2}: {3}")
        @CsvSource({
            "INDEPENDENT, PASSPORT,      MY, true",
            "INDEPENDENT, PASSPORT,      ID, false",
            "INDEPENDENT, IDENTITY_CARD, ID, false",
            "BUSINESS,    PASSPORT,      ID, true",
            "BUSINESS,    IDENTITY_CARD, ID, true"
        })
        void onlyABusinessChoosesTheIssuingCountry(ProviderType type, DocumentType document, String country, boolean waiting) {
            if (waiting) {
                assertThat(startAdding(type, TeamRole.HELPER, document, Country.of(country), "1 March").status())
                        .isEqualTo(MemberVerificationStatus.WAITING);
            } else {
                assertThatThrownBy(() -> startAdding(type, TeamRole.HELPER, document, Country.of(country), "1 March"))
                        .isInstanceOf(DocumentNotAcceptedException.class)
                        .hasMessageContaining(country);
            }
        }

        @Test
        void anIndependentProvidersMembersAreMalaysianWhateverTheListSays() {
            AcceptedDocuments anyCountry = new AcceptedDocuments(Set.of(new AcceptedDocument(
                    DocumentHolder.INDEPENDENT_TEAM_MEMBER, DocumentType.PASSPORT, Optional.empty())));

            assertThatThrownBy(() -> MemberVerification.start(
                            "provider-1", ProviderType.INDEPENDENT, TeamRole.HELPER, DocumentType.PASSPORT, INDONESIA,
                            Photo.of("photos/aina.jpg"), confirmedOn("1 March"), anyCountry, LIMITS, on("1 March")))
                    .isInstanceOf(DocumentNotAcceptedException.class);
            assertThat(MemberVerification.start(
                            "provider-1", ProviderType.INDEPENDENT, TeamRole.HELPER, DocumentType.PASSPORT, Country.MALAYSIA,
                            Photo.of("photos/aina.jpg"), confirmedOn("1 March"), anyCountry, LIMITS, on("1 March")).status())
                    .isEqualTo(MemberVerificationStatus.WAITING);
        }

        @Test
        void aDocumentOffTheListIsRefused() {
            AcceptedDocuments passportsOnly = new AcceptedDocuments(Set.of(new AcceptedDocument(
                    DocumentHolder.BUSINESS_TEAM_MEMBER, DocumentType.PASSPORT, Optional.empty())));

            assertThatThrownBy(() -> MemberVerification.start(
                            "provider-1", ProviderType.BUSINESS, TeamRole.HELPER, DocumentType.IDENTITY_CARD, INDONESIA,
                            Photo.of("photos/aina.jpg"), confirmedOn("1 March"), passportsOnly, LIMITS, on("1 March")))
                    .isInstanceOf(DocumentNotAcceptedException.class)
                    .hasMessageContaining("BUSINESS_TEAM_MEMBER");
        }
    }

    @Nested
    class TheEkycResult {

        @Test
        void aBusinessCanAddAForeignTeamMember() {
            MemberVerification verification =
                    startAdding(ProviderType.BUSINESS, TeamRole.HELPER, DocumentType.PASSPORT, INDONESIA, "1 March");

            verification.recordEkycResult(INDONESIAN_PASSPORT, ACCEPTED, on("2 March"));
            TeamMember ahmad = TeamMember.add(verification, on("2 March"));

            assertThat(ahmad.teamStatus()).isEqualTo(TeamStatus.MEMBER);
            assertThat(ahmad.registeredName()).isEqualTo("Ahmad bin Ismail");
            assertThat(ahmad.document().issuingCountry()).isEqualTo(INDONESIA);
        }

        @Test
        void theVerifiedDocumentMustBeTheTypeChosenOnTheForm() {
            MemberVerification verification = aHelperWaitingSince("1 March");
            EkycResult malaysianPassport =
                    verifiedWith(new IdentityDocument(Country.MALAYSIA, DocumentType.PASSPORT, "Aina binti Ahmad"));

            assertThatThrownBy(() -> verification.recordEkycResult(malaysianPassport, ACCEPTED, on("2 March")))
                    .isInstanceOf(DocumentNotAcceptedException.class);
            assertThat(verification.status()).isEqualTo(MemberVerificationStatus.WAITING);
            assertThat(verification.document()).isEmpty();
        }

        @Test
        void theVerifiedDocumentMustBeFromTheCountryChosenOnTheForm() {
            MemberVerification verification =
                    startAdding(ProviderType.BUSINESS, TeamRole.HELPER, DocumentType.PASSPORT, INDONESIA, "1 March");
            EkycResult malaysianPassport =
                    verifiedWith(new IdentityDocument(Country.MALAYSIA, DocumentType.PASSPORT, "Aina binti Ahmad"));

            assertThatThrownBy(() -> verification.recordEkycResult(malaysianPassport, ACCEPTED, on("2 March")))
                    .isInstanceOf(DocumentNotAcceptedException.class);
            assertThat(verification.status()).isEqualTo(MemberVerificationStatus.WAITING);
        }

        @Test
        void theVerifiedDocumentMustStillBeAccepted() {
            MemberVerification verification =
                    startAdding(ProviderType.BUSINESS, TeamRole.HELPER, DocumentType.PASSPORT, INDONESIA, "1 March");
            AcceptedDocuments malaysianOnly = new AcceptedDocuments(Set.of(new AcceptedDocument(
                    DocumentHolder.BUSINESS_TEAM_MEMBER, DocumentType.PASSPORT, Optional.of(Country.MALAYSIA))));

            assertThatThrownBy(() -> verification.recordEkycResult(INDONESIAN_PASSPORT, malaysianOnly, on("2 March")))
                    .isInstanceOf(DocumentNotAcceptedException.class);
            assertThat(verification.status()).isEqualTo(MemberVerificationStatus.WAITING);
        }

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
        void aRejectedEkycAddsNothing() {
            MemberVerification verification = aHelperWaitingSince("1 March");

            EkycOutcome outcome = verification.recordEkycResult(EkycResult.rejected(), ACCEPTED, on("2 March"));

            assertThat(outcome).isEqualTo(EkycOutcome.REJECTED);
            assertThat(verification.status()).isEqualTo(MemberVerificationStatus.REJECTED);
            assertThat(verification.hasEnded()).isTrue();
            assertThatThrownBy(() -> TeamMember.add(verification, on("2 March"))).isInstanceOf(MemberNotVerifiedException.class);
        }

        @Test
        void aVerificationIsUsedOnce() {
            MemberVerification verification = aHelperWaitingSince("1 March");
            verification.recordEkycResult(verifiedAsAina(), ACCEPTED, on("2 March"));
            TeamMember.add(verification, on("2 March"));

            assertThatThrownBy(() -> TeamMember.add(verification, on("3 March")))
                    .isInstanceOf(MemberAlreadyAddedException.class)
                    .hasMessageContaining(verification.id());
        }

        @Test
        void aRepeatedResultChangesNothing() {
            MemberVerification verified = aHelperWaitingSince("1 March");
            verified.recordEkycResult(verifiedAsAina(), ACCEPTED, on("2 March"));
            MemberVerification rejected = aHelperWaitingSince("1 March");
            rejected.recordEkycResult(EkycResult.rejected(), ACCEPTED, on("2 March"));

            assertThat(verified.recordEkycResult(verifiedAsAina(), ACCEPTED, on("3 March"))).isEqualTo(EkycOutcome.UNCHANGED);
            assertThat(verified.verification().orElseThrow().lastVerified()).isEqualTo(on("2 March"));
            assertThat(rejected.recordEkycResult(EkycResult.rejected(), ACCEPTED, on("3 March"))).isEqualTo(EkycOutcome.UNCHANGED);

            TeamMember.add(verified, on("3 March"));
            assertThat(verified.recordEkycResult(verifiedAsAina(), ACCEPTED, on("4 March"))).isEqualTo(EkycOutcome.UNCHANGED);
        }

        @Test
        void anEndedOrVerifiedVerificationTakesNoDifferentResult() {
            MemberVerification verified = aHelperWaitingSince("1 March");
            verified.recordEkycResult(verifiedAsAina(), ACCEPTED, on("2 March"));
            MemberVerification rejected = aHelperWaitingSince("1 March");
            rejected.recordEkycResult(EkycResult.rejected(), ACCEPTED, on("2 March"));
            MemberVerification expired = aHelperWaitingSince("1 March");
            expired.expire(ELEVEN_ON_1_MARCH);

            assertThatThrownBy(() -> verified.recordEkycResult(EkycResult.rejected(), ACCEPTED, on("3 March")))
                    .isInstanceOf(VerificationNotWaitingException.class)
                    .hasMessageContaining("VERIFIED");
            assertThatThrownBy(() -> rejected.recordEkycResult(verifiedAsAina(), ACCEPTED, on("3 March")))
                    .isInstanceOf(VerificationNotWaitingException.class)
                    .hasMessageContaining("REJECTED");
            assertThatThrownBy(() -> expired.recordEkycResult(verifiedAsAina(), ACCEPTED, on("9 March")))
                    .isInstanceOf(VerificationNotWaitingException.class);
            assertThatThrownBy(() -> expired.recordEkycResult(EkycResult.rejected(), ACCEPTED, on("9 March")))
                    .isInstanceOf(VerificationNotWaitingException.class);
            assertThat(rejected.status()).isEqualTo(MemberVerificationStatus.REJECTED);
        }
    }

    @Nested
    class Expiry {

        @Test
        void anUnfinishedOnTheSpotEkycExpiresAfterAnHour() {
            MemberVerification verification = aHelperWaitingSince("1 March");

            verification.expire(ELEVEN_ON_1_MARCH);

            assertThat(verification.status()).isEqualTo(MemberVerificationStatus.EXPIRED);
            assertThat(verification.hasEnded()).isTrue();
        }

        @Test
        void anOnTheSpotEkycDoesNotExpireBeforeAnHour() {
            MemberVerification verification = aHelperWaitingSince("1 March");

            assertThatThrownBy(() -> verification.expire(ELEVEN_ON_1_MARCH.minusSeconds(60)))
                    .isInstanceOf(VerificationNotExpiredException.class)
                    .hasMessageContaining(ELEVEN_ON_1_MARCH.toString());
            assertThat(verification.status()).isEqualTo(MemberVerificationStatus.WAITING);
        }

        @Test
        void onlyAWaitingVerificationExpires() {
            MemberVerification verified = aHelperWaitingSince("1 March");
            verified.recordEkycResult(verifiedAsAina(), ACCEPTED, on("2 March"));

            assertThatThrownBy(() -> verified.expire(ELEVEN_ON_1_MARCH))
                    .isInstanceOf(VerificationNotWaitingException.class)
                    .hasMessageContaining(verified.id());
            assertThat(verified.status()).isEqualTo(MemberVerificationStatus.VERIFIED);
        }
    }

    @Nested
    class TheTeamMember {

        @Test
        void theDueDateFollowsTheCycleOnly() {
            MemberVerification verification =
                    startAdding(ProviderType.BUSINESS, TeamRole.HELPER, DocumentType.PASSPORT, INDONESIA, "1 January");
            verification.recordEkycResult(INDONESIAN_PASSPORT, ACCEPTED, on("1 January"));

            assertThat(TeamMember.add(verification, on("1 January")).verification().dueAt()).isEqualTo(on("1 April"));
        }

        @Test
        void aPassedRecheckKeepsTheDocumentReadThisTime() {
            TeamMember aina = aTeamMemberSince("1 January");
            aina.openRecheck(LIMITS, on("18 March"));
            EkycResult passport = verifiedWith(new IdentityDocument(Country.MALAYSIA, DocumentType.PASSPORT, "Aina binti Ahmad"));

            aina.recordEkycResult(passport, ACCEPTED, LIMITS, on("20 March"));

            assertThat(aina.document().type()).isEqualTo(DocumentType.PASSPORT);
        }

        @Test
        void aRecheckOfAnIndependentProvidersMemberNeedsAMalaysianDocument() {
            TeamMember aina = aTeamMemberSince("1 January");
            aina.openRecheck(LIMITS, on("18 March"));

            assertThatThrownBy(() -> aina.recordEkycResult(INDONESIAN_PASSPORT, ACCEPTED, LIMITS, on("20 March")))
                    .isInstanceOf(DocumentNotAcceptedException.class);
            assertThat(aina.verification().recheckOpen()).isTrue();
            assertThat(aina.document()).isEqualTo(aMyKad("Aina binti Ahmad"));
        }

        @Test
        void theProfilePhotoIsMatchedOnce() {
            TeamMember aina = aTeamMemberSince("1 January");
            assertThat(aina.photoMatch()).isEmpty();

            aina.recordPhotoMatch(true);

            assertThat(aina.photoMatch()).contains(true);
            assertThatThrownBy(() -> aina.recordPhotoMatch(false))
                    .isInstanceOf(PhotoMatchAlreadyRecordedException.class)
                    .hasMessageContaining(aina.id());
            assertThat(aina.photoMatch()).contains(true);
        }

        @Test
        void aPhotoThatDoesNotMatchIsRecorded() {
            TeamMember aina = aTeamMemberSince("1 January");

            aina.recordPhotoMatch(false);

            assertThat(aina.photoMatch()).contains(false);
        }

        @ParameterizedTest(name = "a {0} member can be removed")
        @EnumSource(value = TeamStatus.class, names = {"MEMBER", "LAPSED"})
        void theProviderCanRemoveATeamMember(TeamStatus status) {
            TeamMember aina = aTeamMemberSince("1 January");
            if (status == TeamStatus.LAPSED) {
                aina.lapse(on("1 April"));
            }

            aina.remove();

            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.REMOVED);
            assertThat(aina.isTeamMember()).isFalse();
        }

        @Test
        void aRemovedMemberIsRemovedOnce() {
            TeamMember aina = aTeamMemberSince("1 January");
            aina.remove();

            assertThatThrownBy(aina::remove).isInstanceOf(TeamMemberRemovedException.class).hasMessageContaining(aina.id());
        }
    }

    @Nested
    class Documents {

        @Test
        void aTeamMemberIsCheckedAgainstTheirProvidersList() {
            assertThat(DocumentHolder.teamMemberOf(ProviderType.INDEPENDENT)).isEqualTo(DocumentHolder.INDEPENDENT_TEAM_MEMBER);
            assertThat(DocumentHolder.teamMemberOf(ProviderType.BUSINESS)).isEqualTo(DocumentHolder.BUSINESS_TEAM_MEMBER);
        }

        @ParameterizedTest(name = "\"{0}\" is not a country")
        @NullSource
        @ValueSource(strings = {"Malaysia", "my", "XX"})
        void aCountryIsAnIsoCode(String code) {
            assertThatThrownBy(() -> Country.of(code)).isInstanceOf(InvalidCountryException.class);
        }

        @Test
        void anAcceptedDocumentMatchesItsHolderTypeAndCountry() {
            assertThat(ACCEPTED.accepts(DocumentHolder.PROVIDER, DocumentType.PASSPORT, Country.MALAYSIA)).isTrue();
            assertThat(ACCEPTED.accepts(DocumentHolder.PROVIDER, DocumentType.PASSPORT, INDONESIA)).isFalse();
            assertThat(ACCEPTED.accepts(DocumentHolder.BUSINESS_TEAM_MEMBER, DocumentType.PASSPORT, INDONESIA)).isTrue();
            AcceptedDocuments providerPassports = new AcceptedDocuments(Set.of(
                    new AcceptedDocument(DocumentHolder.PROVIDER, DocumentType.PASSPORT, Optional.of(Country.MALAYSIA))));
            assertThat(providerPassports.accepts(DocumentHolder.PROVIDER, DocumentType.IDENTITY_CARD, Country.MALAYSIA)).isFalse();
            assertThat(providerPassports.accepts(DocumentHolder.INDEPENDENT_TEAM_MEMBER, DocumentType.PASSPORT, Country.MALAYSIA))
                    .isFalse();
        }

        @Test
        void anAcceptedDocumentNeedsAllItsParts() {
            assertThatThrownBy(() -> new AcceptedDocument(null, DocumentType.PASSPORT, Optional.empty()))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new AcceptedDocument(DocumentHolder.PROVIDER, null, Optional.empty()))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new AcceptedDocument(DocumentHolder.PROVIDER, DocumentType.PASSPORT, null))
                    .isInstanceOf(NullPointerException.class);
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
}
