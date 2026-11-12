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
import account.domain.exception.ProviderNotVerifiedException;
import account.domain.exception.TeamMemberRemovedException;
import account.usecase.AccountUsecaseFixtures.AuditLog;
import account.usecase.AccountUsecaseFixtures.InMemoryMemberVerifications;
import account.usecase.AccountUsecaseFixtures.InMemoryProviders;
import account.usecase.AccountUsecaseFixtures.InMemoryTeamMembers;
import account.usecase.AccountUsecaseFixtures.KycProvider;
import account.usecase.AccountUsecaseFixtures.PublishedEvents;
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
import ie.events.TeamMemberAddedIE;
import ie.events.TeamMemberRemovedIE;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
    private final KycRecordMemberVerificationResult result = new KycRecordMemberVerificationResult(verifications, members, events, clock);
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
        return add.start(new StartAddingTeamMemberRequest(providerId, "HELPER", "photos/aina.jpg", true, TERMS));
    }

    private String ainaIsOnTheTeam() {
        String verificationId = addingAHelper(kemas).verificationId();
        result.record(new RecordMemberVerificationResultRequest(verificationId, true, "NUR AINA BINTI AHMAD", A_YEAR_ON));
        return members.saved.values().stream().reduce((first, newest) -> newest).orElseThrow().id();
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
            assertThat(saved.confirmation().termsVersion()).isEqualTo(TERMS);
            assertThat(saved.confirmation().givenAt()).isEqualTo(NOW);
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
            StartAddingTeamMemberResponse started = add.start(new StartAddingTeamMemberRequest(kemas, "SUPERVISOR", "photos/ben.jpg", true, TERMS));

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
        void theFormCannotBeSentWithoutTheLawfulEmploymentTick() {
            assertThatThrownBy(() -> add.start(new StartAddingTeamMemberRequest(kemas, "HELPER", "photos/aina.jpg", false, TERMS)))
                    .isInstanceOf(EmploymentNotConfirmedException.class);

            assertThat(verifications.saved).isEmpty();
            assertThat(kyc.opened).isEmpty();
            assertThat(events.all).isEmpty();
        }

        @Test
        void theFormNeedsAPhoto() {
            assertThatThrownBy(() -> add.start(new StartAddingTeamMemberRequest(kemas, "HELPER", null, true, TERMS)))
                    .isInstanceOf(AccountDetailRequiredException.class)
                    .hasMessage("A photo is required");
            assertThat(verifications.saved).isEmpty();
        }

        @ParameterizedTest(name = "role \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "LEAD", "helper"})
        void aTeamMemberIsAHelperOrASupervisor(String role) {
            assertThatThrownBy(() -> add.start(new StartAddingTeamMemberRequest(kemas, role, "photos/aina.jpg", true, TERMS)))
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
        void aPassAddsThePersonToTheTeamStraightAwayWithTheirRegisteredName() {
            String verificationId = addingAHelper(kemas).verificationId();
            Instant later = NOW.plus(Duration.ofMinutes(4));

            new KycRecordMemberVerificationResult(verifications, members, events, at(later))
                    .record(new RecordMemberVerificationResultRequest(verificationId, true, "NUR AINA BINTI AHMAD", A_YEAR_ON));

            assertThat(members.saved).hasSize(1);
            TeamMember aina = members.saved.values().iterator().next();
            assertThat(aina.providerId()).isEqualTo(kemas);
            assertThat(aina.registeredName()).isEqualTo("NUR AINA BINTI AHMAD");
            assertThat(aina.role()).isEqualTo(TeamRole.HELPER);
            assertThat(aina.verifiedUntil()).isEqualTo(A_YEAR_ON);
            assertThat(aina.addedAt()).isEqualTo(later);
            assertThat(aina.isTeamMember()).isTrue();
            assertThat(verifications.saved.get(verificationId).status()).isEqualTo(MemberVerificationStatus.ADDED);
            assertThat(verifications.saves).isEqualTo(2);
            assertThat(members.saves).isEqualTo(1);
            assertThat(events.all).last().isEqualTo(new TeamMemberAddedIE(
                    aina.id(), kemas, verificationId, "HELPER", "NUR AINA BINTI AHMAD", TERMS, NOW, later));
        }

        @Test
        void aRejectionAddsNobodyAndIsTold() {
            String verificationId = addingAHelper(kemas).verificationId();

            result.record(new RecordMemberVerificationResultRequest(verificationId, false, null, null));

            assertThat(members.saved).isEmpty();
            assertThat(verifications.saved.get(verificationId).hasEnded()).isTrue();
            assertThat(verifications.saves).isEqualTo(2);
            assertThat(events.all).last().isEqualTo(new MemberVerificationRejectedIE(verificationId, kemas, NOW));
        }

        @Test
        void aRepeatedPassAddsNobodyAgainAndTellsNothing() {
            String verificationId = addingAHelper(kemas).verificationId();
            result.record(new RecordMemberVerificationResultRequest(verificationId, true, "NUR AINA BINTI AHMAD", A_YEAR_ON));
            int told = events.all.size();
            int saves = verifications.saves;

            result.record(new RecordMemberVerificationResultRequest(verificationId, true, "NUR AINA BINTI AHMAD", A_YEAR_ON));

            assertThat(members.saved).hasSize(1);
            assertThat(events.all).hasSize(told);
            assertThat(verifications.saves).isEqualTo(saves);
        }

        @Test
        void aRepeatedRejectionTellsNothing() {
            String verificationId = addingAHelper(kemas).verificationId();
            result.record(new RecordMemberVerificationResultRequest(verificationId, false, null, null));
            int told = events.all.size();

            result.record(new RecordMemberVerificationResultRequest(verificationId, false, null, null));

            assertThat(events.all).hasSize(told);
            assertThat(verifications.saves).isEqualTo(2);
        }

        @Test
        void aResultForAVerificationThatDoesNotExistIsRefused() {
            assertThatThrownBy(() -> result.record(new RecordMemberVerificationResultRequest("no-such-verification", true, "A", A_YEAR_ON)))
                    .isInstanceOf(MemberVerificationNotFoundException.class)
                    .hasMessage("There is no team member verification no-such-verification");
        }

        @Test
        void nobodyIsToldOfAMemberWhoCannotBeSaved() {
            String verificationId = addingAHelper(kemas).verificationId();
            members.somethingElseSavesFirst();
            int told = events.all.size();

            assertThatThrownBy(() -> result.record(new RecordMemberVerificationResultRequest(verificationId, true, "NUR AINA BINTI AHMAD", A_YEAR_ON)))
                    .isInstanceOf(ChangedMeanwhileException.class);
            assertThat(events.all).hasSize(told);
        }
    }

    @Nested
    class TheTeamView {

        @Test
        void aBusinessSeesItsTeamAndWhoIsWaitingButNotRemovedMembersOrOtherTeams() {
            String aina = ainaIsOnTheTeam();
            String ben = ainaIsOnTheTeam();
            new ProviderRemoveTeamMember(members, events, clock).remove(new ProviderRemoveTeamMemberRequest(kemas, ben));
            String waiting = new ProviderStartAddingTeamMember(providers, verifications, kyc, events, at(NOW.plusSeconds(60)))
                    .start(new StartAddingTeamMemberRequest(kemas, "SUPERVISOR", "photos/chong.jpg", true, TERMS))
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
            assertThat(team.waiting()).containsExactly(new ProviderViewTeamResponse.Waiting(waiting, "SUPERVISOR", NOW.plusSeconds(60)));
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
