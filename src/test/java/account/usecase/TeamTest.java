package account.usecase;

import static account.usecase.AccountUsecaseFixtures.NOW;
import static account.usecase.AccountUsecaseFixtures.PHONE;
import static account.usecase.AccountUsecaseFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.BusinessRegistration;
import account.domain.KybResult;
import account.domain.MemberVerification;
import account.domain.MemberVerificationStatus;
import account.domain.PhoneNumber;
import account.domain.PhoneVerification;
import account.domain.ProviderAccount;
import account.domain.TeamMember;
import account.domain.TeamRole;
import account.domain.TeamStatus;
import account.domain.exception.EmploymentNotConfirmedException;
import account.domain.exception.AccountDetailRequiredException;
import account.domain.exception.MemberNotVerifiedException;
import account.domain.exception.ProviderNotVerifiedException;
import account.domain.exception.TeamMemberRemovedException;
import account.usecase.AccountUsecaseFixtures.AuditLog;
import account.usecase.AccountUsecaseFixtures.InMemoryMemberVerifications;
import account.usecase.AccountUsecaseFixtures.InMemoryProviders;
import account.usecase.AccountUsecaseFixtures.InMemoryTeamMembers;
import account.usecase.AccountUsecaseFixtures.KycProvider;
import account.usecase.AccountUsecaseFixtures.PublishedEvents;
import account.usecase.dto.ConfirmTeamMemberRequest;
import account.usecase.dto.ConfirmTeamMemberResponse;
import account.usecase.dto.HomaRemoveTeamMemberRequest;
import account.usecase.dto.ProviderRemoveTeamMemberRequest;
import account.usecase.dto.ProviderViewTeamRequest;
import account.usecase.dto.ProviderViewTeamResponse;
import account.usecase.dto.RecordMemberVerificationResultRequest;
import account.usecase.dto.StartAddingTeamMemberRequest;
import account.usecase.dto.StartAddingTeamMemberResponse;
import account.usecase.dto.StartMemberReverificationRequest;
import account.usecase.exception.AdminReasonRequiredException;
import account.usecase.exception.ChangedMeanwhileException;
import account.usecase.exception.DecidedByRequiredException;
import account.usecase.exception.MemberVerificationNotFoundException;
import account.usecase.exception.NotYourTeamMemberException;
import account.usecase.exception.ProviderNotFoundException;
import account.usecase.exception.TeamMemberNotFoundException;
import account.usecase.exception.UnknownTeamRoleException;
import account.usecase.port.out.AdminAction;
import ie.events.MemberVerificationRejectedIE;
import ie.events.MemberVerificationStartedIE;
import ie.events.MemberVerifiedIE;
import ie.events.TeamMemberAddedIE;
import ie.events.TeamMemberRemovedIE;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** A business builds its team: it adds helpers and supervisors, who verify on the spot, and removes them. */
class TeamTest {

    private static final String TERMS = "employment-terms-2026-11";
    private static final Instant A_YEAR_ON = Instant.parse("2027-11-12T00:00:00Z");
    private static final String FOUNDER = "founder-aisyah";

    private final InMemoryProviders providers = new InMemoryProviders();
    private final InMemoryMemberVerifications verifications = new InMemoryMemberVerifications();
    private final InMemoryTeamMembers members = new InMemoryTeamMembers();
    private final PublishedEvents events = new PublishedEvents();
    private final KycProvider kyc = new KycProvider();
    private final AuditLog auditLog = new AuditLog();
    private final Clock clock = at(NOW);
    private final ProviderStartAddingTeamMember add = new ProviderStartAddingTeamMember(providers, verifications, kyc, events, clock);
    private final KycRecordMemberVerificationResult result = new KycRecordMemberVerificationResult(verifications, events, clock);
    private final ProviderConfirmTeamMember confirm = new ProviderConfirmTeamMember(verifications, members, events, clock);
    private static final String AINA = "NUR AINA BINTI AHMAD";
    private static final LocalDate AN_ADULT = LocalDate.of(1998, 5, 1);
    private final String kemas = aBusiness();
    private final String other = aBusiness();

    private String aBusiness() {
        PhoneVerification phone = PhoneVerification.start(PhoneNumber.of(PHONE), "482915", NOW);
        phone.confirm("482915", NOW);
        ProviderAccount provider = ProviderAccount.register(phone);
        provider.recordKybResult(KybResult.verified(new BusinessRegistration("202601012345", "Kemas Home Care Sdn Bhd"), A_YEAR_ON), NOW);
        providers.saved.put(provider.id(), provider);
        return provider.id();
    }

    private StartAddingTeamMemberResponse addingAHelper(String providerId) {
        return add.start(new StartAddingTeamMemberRequest(providerId, "HELPER", "photos/aina.jpg"));
    }

    private static RecordMemberVerificationResultRequest aPassFor(String verificationId) {
        return new RecordMemberVerificationResultRequest(verificationId, true, AINA, A_YEAR_ON, AN_ADULT, true);
    }

    private static RecordMemberVerificationResultRequest aRejectionFor(String verificationId) {
        return new RecordMemberVerificationResultRequest(verificationId, false, null, null, null, false);
    }

    private String ainaIsVerified() {
        String verificationId = addingAHelper(kemas).verificationId();
        result.record(aPassFor(verificationId));
        return verificationId;
    }

    private String ainaIsOnTheTeam() {
        return confirm.confirm(new ConfirmTeamMemberRequest(kemas, ainaIsVerified(), true, TERMS)).memberId();
    }

    @Nested
    class StartingToAdd {

        @Test
        void theAddFormStartsAVerificationWaitingForThePerson() {
            StartAddingTeamMemberResponse started = addingAHelper(kemas);

            MemberVerification saved = verifications.saved.get(started.verificationId());
            assertThat(saved.providerId()).isEqualTo(kemas);
            assertThat(saved.role()).isEqualTo(TeamRole.HELPER);
            assertThat(saved.photo().reference()).isEqualTo("photos/aina.jpg");
            assertThat(saved.confirmation()).isEmpty();
            assertThat(saved.status()).isEqualTo(MemberVerificationStatus.WAITING);
            assertThat(members.saved).isEmpty();
        }

        @Test
        void theKycProvidersCheckOpensOnTheProvidersPhone() {
            StartAddingTeamMemberResponse started = addingAHelper(kemas);

            assertThat(started.checkUrl()).isEqualTo("https://kyc.example/check/" + started.verificationId());
            assertThat(kyc.opened).containsExactly("check " + started.verificationId());
        }

        @Test
        void theStartIsToldToTheOtherContexts() {
            StartAddingTeamMemberResponse started = add.start(new StartAddingTeamMemberRequest(kemas, "SUPERVISOR", "photos/ben.jpg"));

            assertThat(events.all).containsExactly(new MemberVerificationStartedIE(started.verificationId(), kemas, "SUPERVISOR", NOW));
        }

        @Test
        void aBusinessWhoseKybHasNotPassedCannotStartAdding() {
            PhoneVerification phone = PhoneVerification.start(PhoneNumber.of(PHONE), "482915", NOW);
            phone.confirm("482915", NOW);
            ProviderAccount waiting = ProviderAccount.register(phone);
            providers.saved.put(waiting.id(), waiting);

            assertThatThrownBy(() -> addingAHelper(waiting.id())).isInstanceOf(ProviderNotVerifiedException.class);

            assertThat(verifications.saved).isEmpty();
            assertThat(kyc.opened).isEmpty();
            assertThat(events.all).isEmpty();
        }

        @Test
        void theFormNeedsAPhoto() {
            assertThatThrownBy(() -> add.start(new StartAddingTeamMemberRequest(kemas, "HELPER", null)))
                    .isInstanceOf(AccountDetailRequiredException.class)
                    .hasMessage("A photo is required");
            assertThat(verifications.saved).isEmpty();
        }

        @ParameterizedTest(name = "role \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "LEAD", "helper"})
        void aTeamMemberIsAHelperOrASupervisor(String role) {
            assertThatThrownBy(() -> add.start(new StartAddingTeamMemberRequest(kemas, role, "photos/aina.jpg")))
                    .isInstanceOf(UnknownTeamRoleException.class)
                    .hasMessage("A team member is a HELPER or a SUPERVISOR, was " + role);
            assertThat(verifications.saved).isEmpty();
        }

        @Test
        void aBusinessThatDoesNotExistAddsNobody() {
            assertThatThrownBy(() -> addingAHelper("no-such-provider")).isInstanceOf(ProviderNotFoundException.class);
            assertThat(kyc.opened).isEmpty();
        }

        @Test
        void theCheckDoesNotOpenWhenTheVerificationCannotBeSaved() {
            verifications.somethingElseSavesFirst();

            assertThatThrownBy(() -> addingAHelper(kemas)).isInstanceOf(ChangedMeanwhileException.class);

            assertThat(kyc.opened).isEmpty();
            assertThat(events.all).isEmpty();
        }
    }

    @Nested
    class TheKycProvidersResult {

        @Test
        void aPassWaitsForTheProvidersConfirmationAndIsTold() {
            String verificationId = addingAHelper(kemas).verificationId();
            Instant later = NOW.plus(Duration.ofMinutes(4));

            new KycRecordMemberVerificationResult(verifications, events, at(later)).record(aPassFor(verificationId));

            assertThat(members.saved).isEmpty();
            MemberVerification verified = verifications.saved.get(verificationId);
            assertThat(verified.status()).isEqualTo(MemberVerificationStatus.VERIFIED);
            assertThat(verified.registeredName()).contains(AINA);
            assertThat(verifications.saves).isEqualTo(2);
            assertThat(events.all).last().isEqualTo(new MemberVerifiedIE(verificationId, kemas, AINA, later));
        }

        @Test
        void aRejectionAddsNobodyAndIsToldWithTheReason() {
            String verificationId = addingAHelper(kemas).verificationId();

            result.record(aRejectionFor(verificationId));

            assertThat(verifications.saved.get(verificationId).hasEnded()).isTrue();
            assertThat(verifications.saves).isEqualTo(2);
            assertThat(events.all).last().isEqualTo(new MemberVerificationRejectedIE(verificationId, kemas, "CHECK_REJECTED", NOW));
        }

        @Test
        void aPersonUnder18IsNotAddedAndTheReasonIsTold() {
            String verificationId = addingAHelper(kemas).verificationId();

            result.record(new RecordMemberVerificationResultRequest(verificationId, true, AINA, A_YEAR_ON, LocalDate.of(2012, 1, 1), true));

            assertThat(verifications.saved.get(verificationId).hasEnded()).isTrue();
            assertThat(events.all).last().isEqualTo(new MemberVerificationRejectedIE(verificationId, kemas, "UNDER_18", NOW));
        }

        @Test
        void aPhotoThatIsNotThePersonIsRefusedAndTheReasonIsTold() {
            String verificationId = addingAHelper(kemas).verificationId();

            result.record(new RecordMemberVerificationResultRequest(verificationId, true, AINA, A_YEAR_ON, AN_ADULT, false));

            assertThat(events.all).last().isEqualTo(new MemberVerificationRejectedIE(verificationId, kemas, "PHOTO_DOES_NOT_MATCH", NOW));
        }

        @Test
        void aRepeatedPassTellsNothing() {
            String verificationId = ainaIsVerified();
            int told = events.all.size();
            int saves = verifications.saves;

            result.record(aPassFor(verificationId));

            assertThat(events.all).hasSize(told);
            assertThat(verifications.saves).isEqualTo(saves);
        }

        @Test
        void aRepeatedRejectionTellsNothing() {
            String verificationId = addingAHelper(kemas).verificationId();
            result.record(aRejectionFor(verificationId));
            int told = events.all.size();

            result.record(aRejectionFor(verificationId));

            assertThat(events.all).hasSize(told);
            assertThat(verifications.saves).isEqualTo(2);
        }

        @Test
        void aResultForAVerificationThatDoesNotExistIsRefused() {
            assertThatThrownBy(() -> result.record(aPassFor("no-such-verification")))
                    .isInstanceOf(MemberVerificationNotFoundException.class)
                    .hasMessage("There is no team member verification no-such-verification");
        }

        @Test
        void nobodyIsToldOfAResultThatCannotBeSaved() {
            String verificationId = addingAHelper(kemas).verificationId();
            verifications.somethingElseSavesFirst();
            int told = events.all.size();

            assertThatThrownBy(() -> result.record(aPassFor(verificationId))).isInstanceOf(ChangedMeanwhileException.class);
            assertThat(events.all).hasSize(told);
        }
    }

    @Nested
    class Confirming {

        @Test
        void theProviderConfirmsTheNamedPersonAndTheyJoinWithTheirRegisteredName() {
            String verificationId = ainaIsVerified();
            Instant later = NOW.plus(Duration.ofMinutes(6));

            ConfirmTeamMemberResponse joined = new ProviderConfirmTeamMember(verifications, members, events, at(later))
                    .confirm(new ConfirmTeamMemberRequest(kemas, verificationId, true, TERMS));

            TeamMember aina = members.saved.get(joined.memberId());
            assertThat(aina.providerId()).isEqualTo(kemas);
            assertThat(aina.registeredName()).isEqualTo(AINA);
            assertThat(aina.role()).isEqualTo(TeamRole.HELPER);
            assertThat(aina.verifiedUntil()).isEqualTo(A_YEAR_ON);
            assertThat(aina.addedAt()).isEqualTo(later);
            assertThat(aina.employmentConfirmation().termsVersion()).isEqualTo(TERMS);
            assertThat(aina.employmentConfirmation().givenAt()).isEqualTo(later);
            assertThat(verifications.saved.get(verificationId).status()).isEqualTo(MemberVerificationStatus.ADDED);
            assertThat(members.saves).isEqualTo(1);
            assertThat(verifications.saves).isEqualTo(3);
            assertThat(events.all).last().isEqualTo(new TeamMemberAddedIE(
                    aina.id(), kemas, verificationId, "HELPER", AINA, TERMS, later, later));
        }

        @Test
        void nobodyJoinsWithoutTheTick() {
            String verificationId = ainaIsVerified();
            int told = events.all.size();

            assertThatThrownBy(() -> confirm.confirm(new ConfirmTeamMemberRequest(kemas, verificationId, false, TERMS)))
                    .isInstanceOf(EmploymentNotConfirmedException.class);

            assertThat(members.saved).isEmpty();
            assertThat(events.all).hasSize(told);
        }

        @Test
        void thePersonIsConfirmedOnlyOnceVerified() {
            String verificationId = addingAHelper(kemas).verificationId();

            assertThatThrownBy(() -> confirm.confirm(new ConfirmTeamMemberRequest(kemas, verificationId, true, TERMS)))
                    .isInstanceOf(MemberNotVerifiedException.class);
            assertThat(members.saved).isEmpty();
        }

        @Test
        void aBusinessConfirmsOnlyItsOwnPeople() {
            String verificationId = ainaIsVerified();

            assertThatThrownBy(() -> confirm.confirm(new ConfirmTeamMemberRequest(other, verificationId, true, TERMS)))
                    .isInstanceOf(MemberVerificationNotFoundException.class);
            assertThatThrownBy(() -> confirm.confirm(new ConfirmTeamMemberRequest(kemas, "no-such-verification", true, TERMS)))
                    .isInstanceOf(MemberVerificationNotFoundException.class);
            assertThat(members.saved).isEmpty();
        }

        @Test
        void nobodyIsToldOfAMemberWhoCannotBeSaved() {
            String verificationId = ainaIsVerified();
            members.somethingElseSavesFirst();
            int told = events.all.size();

            assertThatThrownBy(() -> confirm.confirm(new ConfirmTeamMemberRequest(kemas, verificationId, true, TERMS)))
                    .isInstanceOf(ChangedMeanwhileException.class);
            assertThat(events.all).hasSize(told);
        }
    }

    @Nested
    class TheTeamView {

        @Test
        void aVerifiedPersonWaitsForTheProvidersConfirmationWithTheirName() {
            String verificationId = ainaIsVerified();

            ProviderViewTeamResponse team = new ProviderViewTeam(members, verifications).view(new ProviderViewTeamRequest(kemas));

            assertThat(team.members()).isEmpty();
            assertThat(team.waiting()).containsExactly(new ProviderViewTeamResponse.Waiting(verificationId, "HELPER", "VERIFIED", AINA, NOW));
        }

        @Test
        void aBusinessSeesItsTeamAndWhoIsWaitingButNotRemovedMembersOrOtherTeams() {
            String aina = ainaIsOnTheTeam();
            String ben = ainaIsOnTheTeam();
            new ProviderRemoveTeamMember(members, events, clock).remove(new ProviderRemoveTeamMemberRequest(kemas, ben));
            String waiting = new ProviderStartAddingTeamMember(providers, verifications, kyc, events, at(NOW.plusSeconds(60)))
                    .start(new StartAddingTeamMemberRequest(kemas, "SUPERVISOR", "photos/chong.jpg"))
                    .verificationId();
            addingAHelper(other);

            ProviderViewTeamResponse team = new ProviderViewTeam(members, verifications).view(new ProviderViewTeamRequest(kemas));

            assertThat(team.members()).containsExactly(
                    new ProviderViewTeamResponse.Member(aina, "NUR AINA BINTI AHMAD", "HELPER", "MEMBER", A_YEAR_ON, NOW));
            ProviderViewTeamResponse.Member member = team.members().get(0);
            assertThat(member.memberId()).isEqualTo(aina);
            assertThat(member.registeredName()).isEqualTo("NUR AINA BINTI AHMAD");
            assertThat(member.role()).isEqualTo("HELPER");
            assertThat(member.status()).isEqualTo("MEMBER");
            assertThat(member.verifiedUntil()).isEqualTo(A_YEAR_ON);
            assertThat(member.lastVerifiedAt()).isEqualTo(NOW);
            assertThat(team.waiting()).containsExactly(
                    new ProviderViewTeamResponse.Waiting(waiting, "SUPERVISOR", "WAITING", "", NOW.plusSeconds(60)));
            assertThat(team.waiting().get(0).stage()).isEqualTo("WAITING");
            assertThat(team.waiting().get(0).registeredName()).isEmpty();
            assertThat(team.waiting().get(0).verificationId()).isEqualTo(waiting);
            assertThat(team.waiting().get(0).role()).isEqualTo("SUPERVISOR");
            assertThat(team.waiting().get(0).startedAt()).isEqualTo(NOW.plusSeconds(60));
        }

        @Test
        void aLapsedMemberIsShownAsLapsed() {
            String aina = ainaIsOnTheTeam();
            new KycRecordMemberLapse(members, events, clock).record(new account.usecase.dto.RecordMemberLapseRequest(aina));

            ProviderViewTeamResponse team = new ProviderViewTeam(members, verifications).view(new ProviderViewTeamRequest(kemas));

            assertThat(team.members()).extracting(ProviderViewTeamResponse.Member::status).containsExactly("LAPSED");
        }

        @Test
        void aNewBusinessHasNoTeam() {
            ProviderViewTeamResponse team = new ProviderViewTeam(members, verifications).view(new ProviderViewTeamRequest(kemas));

            assertThat(team.members()).isEmpty();
            assertThat(team.waiting()).isEmpty();
        }
    }

    @Nested
    class Removing {

        @Test
        void aBusinessRemovesItsTeamMemberAndItIsTold() {
            String aina = ainaIsOnTheTeam();
            int saves = members.saves;

            new ProviderRemoveTeamMember(members, events, clock).remove(new ProviderRemoveTeamMemberRequest(kemas, aina));

            assertThat(members.saved.get(aina).teamStatus()).isEqualTo(TeamStatus.REMOVED);
            assertThat(members.saves).isEqualTo(saves + 1);
            assertThat(events.all).last().isEqualTo(new TeamMemberRemovedIE(aina, kemas, "HELPER", NOW));
        }

        @Test
        void aBusinessCannotRemoveAnotherBusinessesTeamMember() {
            String aina = ainaIsOnTheTeam();

            assertThatThrownBy(() -> new ProviderRemoveTeamMember(members, events, clock).remove(new ProviderRemoveTeamMemberRequest(other, aina)))
                    .isInstanceOf(NotYourTeamMemberException.class)
                    .hasMessage("Team member " + aina + " is not on the team of provider " + other);
            assertThat(members.saved.get(aina).isTeamMember()).isTrue();
        }

        @Test
        void aRemovedMemberIsNotRemovedAgain() {
            String aina = ainaIsOnTheTeam();
            ProviderRemoveTeamMember remove = new ProviderRemoveTeamMember(members, events, clock);
            remove.remove(new ProviderRemoveTeamMemberRequest(kemas, aina));
            int told = events.all.size();

            assertThatThrownBy(() -> remove.remove(new ProviderRemoveTeamMemberRequest(kemas, aina)))
                    .isInstanceOf(TeamMemberNotFoundException.class)
                    .hasMessage("There is no team member " + aina);
            assertThat(events.all).hasSize(told);
        }

        @Test
        void aMemberThatDoesNotExistIsNotRemoved() {
            assertThatThrownBy(() -> new ProviderRemoveTeamMember(members, events, clock).remove(new ProviderRemoveTeamMemberRequest(kemas, "no-such-member")))
                    .isInstanceOf(TeamMemberNotFoundException.class);
        }

        @Test
        void homaRemovesATeamMemberWhoMayNotWorkWithAReasonAndRecordsIt() {
            String aina = ainaIsOnTheTeam();

            new HomaRemoveTeamMember(members, events, auditLog, clock)
                    .remove(new HomaRemoveTeamMemberRequest(" " + FOUNDER + " ", aina, "  Not allowed to work in Malaysia "));

            assertThat(members.saved.get(aina).teamStatus()).isEqualTo(TeamStatus.REMOVED);
            assertThat(events.all).last().isEqualTo(new TeamMemberRemovedIE(aina, kemas, "HELPER", NOW));
            assertThat(auditLog.all).containsExactly(
                    new AdminAction(FOUNDER, "REMOVE_TEAM_MEMBER", aina, "Not allowed to work in Malaysia", NOW));
        }

        @ParameterizedTest(name = "reason \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "  "})
        void homaNeedsAReasonToRemoveATeamMember(String reason) {
            String aina = ainaIsOnTheTeam();

            assertThatThrownBy(() -> new HomaRemoveTeamMember(members, events, auditLog, clock)
                    .remove(new HomaRemoveTeamMemberRequest(FOUNDER, aina, reason)))
                    .isInstanceOf(AdminReasonRequiredException.class)
                    .hasMessage("A reason is required to remove a team member");
            assertThat(members.saved.get(aina).isTeamMember()).isTrue();
            assertThat(auditLog.all).isEmpty();
        }

        @ParameterizedTest(name = "decided by \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "  "})
        void homaNeedsTheFounderWhoDecidedIt(String decidedBy) {
            String aina = ainaIsOnTheTeam();

            assertThatThrownBy(() -> new HomaRemoveTeamMember(members, events, auditLog, clock)
                    .remove(new HomaRemoveTeamMemberRequest(decidedBy, aina, "Not allowed to work")))
                    .isInstanceOf(DecidedByRequiredException.class)
                    .hasMessage("An admin action needs the founder who decided it");
            assertThat(members.saved.get(aina).isTeamMember()).isTrue();
        }

        @Test
        void homaCannotRemoveAMemberTwice() {
            String aina = ainaIsOnTheTeam();
            HomaRemoveTeamMember remove = new HomaRemoveTeamMember(members, events, auditLog, clock);
            remove.remove(new HomaRemoveTeamMemberRequest(FOUNDER, aina, "Not allowed to work"));

            assertThatThrownBy(() -> remove.remove(new HomaRemoveTeamMemberRequest(FOUNDER, aina, "Again")))
                    .isInstanceOf(TeamMemberRemovedException.class);
            assertThat(auditLog.all).hasSize(1);
        }

        @Test
        void nothingIsToldOrRecordedWhenTheRemovalCannotBeSaved() {
            String aina = ainaIsOnTheTeam();
            members.somethingElseSavesFirst();
            int told = events.all.size();

            assertThatThrownBy(() -> new HomaRemoveTeamMember(members, events, auditLog, clock)
                    .remove(new HomaRemoveTeamMemberRequest(FOUNDER, aina, "Not allowed to work")))
                    .isInstanceOf(ChangedMeanwhileException.class);
            assertThat(events.all).hasSize(told);
            assertThat(auditLog.all).isEmpty();
        }
    }

    @Nested
    class Reverifying {

        @Test
        void aBusinessOpensItsTeamMembersReverification() {
            String aina = ainaIsOnTheTeam();

            String url = new ProviderStartMemberReverification(members, kyc).start(new StartMemberReverificationRequest(kemas, aina))
                    .reverificationUrl();

            assertThat(url).isEqualTo("https://kyc.example/reverify/" + aina);
            assertThat(kyc.opened).last().isEqualTo("reverify " + aina);
        }

        @Test
        void aBusinessCannotReverifyAnotherBusinessesTeamMember() {
            String aina = ainaIsOnTheTeam();

            assertThatThrownBy(() -> new ProviderStartMemberReverification(members, kyc).start(new StartMemberReverificationRequest(other, aina)))
                    .isInstanceOf(NotYourTeamMemberException.class);
            assertThat(kyc.opened).noneMatch(opened -> opened.startsWith("reverify"));
        }

        @Test
        void aRemovedMemberIsNotReverified() {
            String aina = ainaIsOnTheTeam();
            new ProviderRemoveTeamMember(members, events, clock).remove(new ProviderRemoveTeamMemberRequest(kemas, aina));

            assertThatThrownBy(() -> new ProviderStartMemberReverification(members, kyc).start(new StartMemberReverificationRequest(kemas, aina)))
                    .isInstanceOf(TeamMemberNotFoundException.class);
        }
    }
}
