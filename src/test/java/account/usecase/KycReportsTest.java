package account.usecase;

import static account.usecase.AccountUsecaseFixtures.NOW;
import static account.usecase.AccountUsecaseFixtures.PHONE;
import static account.usecase.AccountUsecaseFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.BusinessRegistration;
import account.domain.KybResult;
import account.domain.PhoneNumber;
import account.domain.PhoneVerification;
import account.domain.ProviderAccount;
import account.domain.TeamMember;
import account.domain.TeamStatus;
import account.domain.VerificationStatus;
import account.domain.exception.AccountDetailRequiredException;
import account.domain.exception.ProviderNotVerifiedException;
import account.domain.exception.VerificationDetailsRequiredException;
import account.usecase.AccountUsecaseFixtures.AuditLog;
import account.usecase.AccountUsecaseFixtures.InMemoryMemberVerifications;
import account.usecase.AccountUsecaseFixtures.InMemoryProviders;
import account.usecase.AccountUsecaseFixtures.InMemoryTeamMembers;
import account.usecase.AccountUsecaseFixtures.KycProvider;
import account.usecase.AccountUsecaseFixtures.PublishedEvents;
import account.usecase.dto.ConfirmTeamMemberRequest;
import account.usecase.dto.HomaRequestBusinessRecheckRequest;
import account.usecase.dto.HomaRequestMemberRecheckRequest;
import account.usecase.dto.RecordBusinessFlagRequest;
import account.usecase.dto.RecordBusinessLapseRequest;
import account.usecase.dto.RecordBusinessReverificationRequiredRequest;
import account.usecase.dto.RecordKybResultRequest;
import account.usecase.dto.RecordMemberFlagRequest;
import account.usecase.dto.RecordMemberLapseRequest;
import account.usecase.dto.RecordMemberReverificationRequiredRequest;
import account.usecase.dto.RecordMemberVerificationResultRequest;
import account.usecase.dto.RecordTeamMemberResultRequest;
import account.usecase.dto.StartAddingTeamMemberRequest;
import account.usecase.exception.AdminReasonRequiredException;
import account.usecase.exception.ChangedMeanwhileException;
import account.usecase.exception.DecidedByRequiredException;
import account.usecase.exception.ProviderNotFoundException;
import account.usecase.exception.TeamMemberNotFoundException;
import account.usecase.port.out.AdminAction;
import ie.events.ProviderFlaggedIE;
import ie.events.ProviderReverifiedIE;
import ie.events.ProviderVerificationLapsedIE;
import ie.events.ProviderVerificationRejectedIE;
import ie.events.ProviderVerifiedIE;
import ie.events.RecheckFailedIE;
import ie.events.ReverificationRequiredIE;
import ie.events.TeamMemberLapsedIE;
import ie.events.TeamMemberReverifiedIE;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** The KYC provider reports results, lapses and what its monitoring finds; Homa records each and tells one event for each outcome (R1). */
class KycReportsTest {

    private static final Instant A_YEAR_ON = Instant.parse("2027-11-12T00:00:00Z");
    private static final Instant TWO_YEARS_ON = Instant.parse("2028-11-12T00:00:00Z");
    private static final Instant NEXT_MONTH = Instant.parse("2026-12-12T00:00:00Z");
    private static final String SSM = "202601012345";
    private static final String KEMAS = "Kemas Home Care Sdn Bhd";
    private static final String FOUNDER = "founder-aisyah";

    private final InMemoryProviders providers = new InMemoryProviders();
    private final InMemoryMemberVerifications verifications = new InMemoryMemberVerifications();
    private final InMemoryTeamMembers members = new InMemoryTeamMembers();
    private final PublishedEvents events = new PublishedEvents();
    private final KycProvider kyc = new KycProvider();
    private final AuditLog auditLog = new AuditLog();
    private final Clock clock = at(NOW);
    private final KycRecordBusinessResult businessResult = new KycRecordBusinessResult(providers, events, clock);
    private final String kemas = aBusiness();

    private String aBusiness() {
        PhoneVerification phone = PhoneVerification.start(PhoneNumber.of(PHONE), "482915", NOW);
        phone.confirm("482915", NOW);
        ProviderAccount provider = ProviderAccount.register(phone);
        providers.saved.put(provider.id(), provider);
        return provider.id();
    }

    private ProviderAccount kemas() {
        return providers.saved.get(kemas);
    }

    private void kybPassesUntil(Instant until) {
        businessResult.record(new RecordKybResultRequest(kemas, true, SSM, KEMAS, until));
    }

    private String ainaIsOnTheTeam() {
        if (kemas().verificationStatus() == VerificationStatus.PENDING) {
            kemas().recordKybResult(KybResult.verified(new BusinessRegistration(SSM, KEMAS), A_YEAR_ON), NOW);
        }
        String verificationId = new ProviderStartAddingTeamMember(providers, verifications, kyc, events, clock)
                .start(new StartAddingTeamMemberRequest(kemas, "HELPER", "photos/aina.jpg")).verificationId();
        new KycRecordMemberVerificationResult(verifications, events, clock).record(new RecordMemberVerificationResultRequest(
                verificationId, true, "NUR AINA BINTI AHMAD", A_YEAR_ON, LocalDate.of(1998, 5, 1), true));
        String memberId = new ProviderConfirmTeamMember(verifications, members, events, clock)
                .confirm(new ConfirmTeamMemberRequest(kemas, verificationId, true, "terms-1")).memberId();
        events.all.clear();
        return memberId;
    }

    @Nested
    class ABusinessResult {

        @Test
        void aPassVerifiesTheBusinessAndIsTold() {
            kybPassesUntil(A_YEAR_ON);

            assertThat(kemas().verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
            assertThat(kemas().businessRegistration().orElseThrow().registeredName()).isEqualTo(KEMAS);
            assertThat(providers.saves).isEqualTo(1);
            assertThat(events.all).containsExactly(new ProviderVerifiedIE(kemas, SSM, A_YEAR_ON, NOW));
        }

        @Test
        void aRejectionIsSavedAndTold() {
            businessResult.record(new RecordKybResultRequest(kemas, false, null, null, null));

            assertThat(kemas().verificationStatus()).isEqualTo(VerificationStatus.REJECTED);
            assertThat(providers.saves).isEqualTo(1);
            assertThat(events.all).containsExactly(new ProviderVerificationRejectedIE(kemas, NOW));
        }

        @Test
        void aPassAtReverificationMovesTheDateAndIsToldAsAReverification() {
            kybPassesUntil(A_YEAR_ON);

            kybPassesUntil(TWO_YEARS_ON);

            assertThat(kemas().verifiedUntil()).contains(TWO_YEARS_ON);
            assertThat(providers.saves).isEqualTo(2);
            assertThat(events.all).last().isEqualTo(new ProviderReverifiedIE(kemas, TWO_YEARS_ON, NOW));
        }

        @Test
        void aFailedReverificationChangesNothingButIsToldSoTheBusinessCanRetry() {
            kybPassesUntil(A_YEAR_ON);

            businessResult.record(new RecordKybResultRequest(kemas, false, null, null, null));

            assertThat(kemas().verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
            assertThat(events.all).last().isEqualTo(new RecheckFailedIE(kemas, "", NOW));
        }

        @Test
        void aRepeatedPassIsNotSavedOrTold() {
            kybPassesUntil(A_YEAR_ON);

            kybPassesUntil(A_YEAR_ON);

            assertThat(providers.saves).isEqualTo(1);
            assertThat(events.all).hasSize(1);
        }

        @Test
        void aPassWithoutTheSsmRegistrationIsRefused() {
            assertThatThrownBy(() -> businessResult.record(new RecordKybResultRequest(kemas, true, null, KEMAS, A_YEAR_ON)))
                    .isInstanceOf(AccountDetailRequiredException.class);

            assertThat(kemas().verificationStatus()).isEqualTo(VerificationStatus.PENDING);
            assertThat(events.all).isEmpty();
        }

        @Test
        void aPassWithoutAVerifiedUntilDateIsRefused() {
            assertThatThrownBy(() -> businessResult.record(new RecordKybResultRequest(kemas, true, SSM, KEMAS, null)))
                    .isInstanceOf(VerificationDetailsRequiredException.class);
            assertThat(events.all).isEmpty();
        }

        @Test
        void aResultForABusinessThatDoesNotExistIsRefused() {
            assertThatThrownBy(() -> businessResult.record(new RecordKybResultRequest("no-such-provider", true, SSM, KEMAS, A_YEAR_ON)))
                    .isInstanceOf(ProviderNotFoundException.class);
        }

        @Test
        void nothingIsToldWhenTheResultCannotBeSaved() {
            providers.somethingElseSavesFirst();

            assertThatThrownBy(() -> kybPassesUntil(A_YEAR_ON)).isInstanceOf(ChangedMeanwhileException.class);
            assertThat(events.all).isEmpty();
        }
    }

    @Nested
    class ATeamMemberResult {

        private final KycRecordTeamMemberResult memberResult = new KycRecordTeamMemberResult(members, events, clock);

        @Test
        void aPassAtReverificationMovesTheDateAndIsTold() {
            String aina = ainaIsOnTheTeam();
            int saves = members.saves;

            memberResult.record(new RecordTeamMemberResultRequest(aina, true, "NUR AINA BINTI AHMAD", TWO_YEARS_ON));

            assertThat(members.saved.get(aina).verifiedUntil()).isEqualTo(TWO_YEARS_ON);
            assertThat(members.saves).isEqualTo(saves + 1);
            assertThat(events.all).containsExactly(new TeamMemberReverifiedIE(aina, kemas, "HELPER", TWO_YEARS_ON, NOW));
        }

        @Test
        void aFailedReverificationChangesNothingButIsTold() {
            String aina = ainaIsOnTheTeam();
            int saves = members.saves;

            memberResult.record(new RecordTeamMemberResultRequest(aina, false, null, null));

            assertThat(members.saved.get(aina).verifiedUntil()).isEqualTo(A_YEAR_ON);
            assertThat(members.saves).isEqualTo(saves);
            assertThat(events.all).containsExactly(new RecheckFailedIE(kemas, aina, NOW));
        }

        @Test
        void aRepeatedPassIsNotSavedOrTold() {
            String aina = ainaIsOnTheTeam();
            int saves = members.saves;

            memberResult.record(new RecordTeamMemberResultRequest(aina, true, "NUR AINA BINTI AHMAD", A_YEAR_ON));

            assertThat(members.saves).isEqualTo(saves);
            assertThat(events.all).isEmpty();
        }

        @Test
        void aLapsedMemberWhoPassesIsBackOnTheTeam() {
            String aina = ainaIsOnTheTeam();
            new KycRecordMemberLapse(members, events, clock).record(new RecordMemberLapseRequest(aina));

            memberResult.record(new RecordTeamMemberResultRequest(aina, true, "NUR AINA BINTI AHMAD", A_YEAR_ON));

            assertThat(members.saved.get(aina).isTeamMember()).isTrue();
            assertThat(events.all).last().isEqualTo(new TeamMemberReverifiedIE(aina, kemas, "HELPER", A_YEAR_ON, NOW));
        }

        @Test
        void aResultForAMemberThatDoesNotExistIsRefused() {
            assertThatThrownBy(() -> memberResult.record(new RecordTeamMemberResultRequest("no-such-member", false, null, null)))
                    .isInstanceOf(TeamMemberNotFoundException.class);
        }
    }

    @Nested
    class LapsesAndMonitoring {

        @Test
        void aBusinessThatMissedItsReverificationLapsesAndIsTold() {
            kybPassesUntil(A_YEAR_ON);

            new KycRecordBusinessLapse(providers, events, clock).record(new RecordBusinessLapseRequest(kemas));

            assertThat(kemas().verificationStatus()).isEqualTo(VerificationStatus.LAPSED);
            assertThat(providers.saves).isEqualTo(2);
            assertThat(events.all).last().isEqualTo(new ProviderVerificationLapsedIE(kemas, NOW));
        }

        @Test
        void aBusinessThatWasNeverVerifiedCannotLapse() {
            assertThatThrownBy(() -> new KycRecordBusinessLapse(providers, events, clock).record(new RecordBusinessLapseRequest(kemas)))
                    .isInstanceOf(ProviderNotVerifiedException.class);
            assertThat(events.all).isEmpty();
        }

        @Test
        void aTeamMemberWhoMissedTheirReverificationLeavesTheTeamAndIsTold() {
            String aina = ainaIsOnTheTeam();
            int saves = members.saves;

            new KycRecordMemberLapse(members, events, clock).record(new RecordMemberLapseRequest(aina));

            assertThat(members.saved.get(aina).teamStatus()).isEqualTo(TeamStatus.LAPSED);
            assertThat(members.saves).isEqualTo(saves + 1);
            assertThat(events.all).containsExactly(new TeamMemberLapsedIE(aina, kemas, "HELPER", NOW));
        }

        @Test
        void aRequiredReverificationMovesTheBusinessesDateSoonerAndIsTold() {
            kybPassesUntil(A_YEAR_ON);

            new KycRecordBusinessReverificationRequired(providers, events, clock)
                    .record(new RecordBusinessReverificationRequiredRequest(kemas, " New director ", NEXT_MONTH));

            assertThat(kemas().verifiedUntil()).contains(NEXT_MONTH);
            assertThat(kemas().verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
            assertThat(providers.saves).isEqualTo(2);
            assertThat(events.all).last().isEqualTo(new ReverificationRequiredIE(kemas, "", "New director", NEXT_MONTH, NOW));
        }

        @Test
        void aRequiredReverificationNeverMovesTheDateLater() {
            kybPassesUntil(NEXT_MONTH);

            new KycRecordBusinessReverificationRequired(providers, events, clock)
                    .record(new RecordBusinessReverificationRequiredRequest(kemas, "New director", A_YEAR_ON));

            assertThat(events.all).last().isEqualTo(new ReverificationRequiredIE(kemas, "", "New director", NEXT_MONTH, NOW));
        }

        @Test
        void aRequiredReverificationMovesATeamMembersDateSoonerAndIsTold() {
            String aina = ainaIsOnTheTeam();
            int saves = members.saves;

            new KycRecordMemberReverificationRequired(members, events, clock)
                    .record(new RecordMemberReverificationRequiredRequest(aina, "Identity document expires", NEXT_MONTH));

            assertThat(members.saved.get(aina).verifiedUntil()).isEqualTo(NEXT_MONTH);
            assertThat(members.saves).isEqualTo(saves + 1);
            assertThat(events.all).containsExactly(
                    new ReverificationRequiredIE(kemas, aina, "Identity document expires", NEXT_MONTH, NOW));
        }

        @Test
        void aSeriouslyFlaggedBusinessLapsesAtOnceAndIsTold() {
            kybPassesUntil(A_YEAR_ON);

            new KycRecordBusinessFlag(providers, events, clock).record(new RecordBusinessFlagRequest(kemas, " Struck off "));

            assertThat(kemas().verificationStatus()).isEqualTo(VerificationStatus.LAPSED);
            assertThat(kemas().seriousFlag()).contains("Struck off");
            assertThat(providers.saves).isEqualTo(2);
            assertThat(events.all).last().isEqualTo(new ProviderFlaggedIE(kemas, "Struck off", NOW));
        }

        @Test
        void aSeriouslyFlaggedTeamMemberLapsesAtOnceAndIsTold() {
            String aina = ainaIsOnTheTeam();
            int saves = members.saves;

            new KycRecordMemberFlag(members, events, clock).record(new RecordMemberFlagRequest(aina, "Sanctions match"));

            assertThat(members.saves).isEqualTo(saves + 1);

            TeamMember member = members.saved.get(aina);
            assertThat(member.teamStatus()).isEqualTo(TeamStatus.LAPSED);
            assertThat(events.all).containsExactly(new TeamMemberLapsedIE(aina, kemas, "HELPER", NOW));
        }

        @Test
        void nothingIsToldWhenAFlagCannotBeSaved() {
            kybPassesUntil(A_YEAR_ON);
            providers.somethingElseSavesFirst();

            assertThatThrownBy(() -> new KycRecordBusinessFlag(providers, events, clock).record(new RecordBusinessFlagRequest(kemas, "Struck off")))
                    .isInstanceOf(ChangedMeanwhileException.class);
            assertThat(events.all).hasSize(1);
        }
    }

    @Nested
    class EarlyRechecks {

        @Test
        void homaAsksForAnEarlyRecheckOfABusinessWithAReasonAndRecordsIt() {
            new HomaRequestBusinessRecheck(providers, kyc, auditLog, clock)
                    .request(new HomaRequestBusinessRecheckRequest(FOUNDER, kemas, " Safety report from a customer "));

            assertThat(kyc.opened).containsExactly("recheck business " + kemas + ": Safety report from a customer");
            assertThat(auditLog.all).containsExactly(
                    new AdminAction(FOUNDER, "REQUEST_BUSINESS_RECHECK", kemas, "Safety report from a customer", NOW));
            assertThat(providers.saves).isZero();
        }

        @Test
        void homaAsksForAnEarlyRecheckOfATeamMemberWithAReasonAndRecordsIt() {
            String aina = ainaIsOnTheTeam();

            new HomaRequestMemberRecheck(members, kyc, auditLog, clock)
                    .request(new HomaRequestMemberRecheckRequest(FOUNDER, aina, "Safety report from a customer"));

            assertThat(kyc.opened).last().isEqualTo("recheck member " + aina + ": Safety report from a customer");
            assertThat(auditLog.all).containsExactly(
                    new AdminAction(FOUNDER, "REQUEST_MEMBER_RECHECK", aina, "Safety report from a customer", NOW));
        }

        @Test
        void anEarlyRecheckNeedsAReason() {
            assertThatThrownBy(() -> new HomaRequestBusinessRecheck(providers, kyc, auditLog, clock)
                    .request(new HomaRequestBusinessRecheckRequest(FOUNDER, kemas, " ")))
                    .isInstanceOf(AdminReasonRequiredException.class)
                    .hasMessage("A reason is required to ask for an early re-check");
            assertThatThrownBy(() -> new HomaRequestMemberRecheck(members, kyc, auditLog, clock)
                    .request(new HomaRequestMemberRecheckRequest(FOUNDER, "any", null)))
                    .isInstanceOf(AdminReasonRequiredException.class);
            assertThat(kyc.opened).isEmpty();
            assertThat(auditLog.all).isEmpty();
        }

        @Test
        void anEarlyRecheckNeedsTheFounderWhoDecidedIt() {
            assertThatThrownBy(() -> new HomaRequestBusinessRecheck(providers, kyc, auditLog, clock)
                    .request(new HomaRequestBusinessRecheckRequest(null, kemas, "Safety report")))
                    .isInstanceOf(DecidedByRequiredException.class);
            assertThatThrownBy(() -> new HomaRequestMemberRecheck(members, kyc, auditLog, clock)
                    .request(new HomaRequestMemberRecheckRequest("", "any", "Safety report")))
                    .isInstanceOf(DecidedByRequiredException.class);
            assertThat(kyc.opened).isEmpty();
        }

        @Test
        void anEarlyRecheckIsOnlyForABusinessOrMemberThatExists() {
            assertThatThrownBy(() -> new HomaRequestBusinessRecheck(providers, kyc, auditLog, clock)
                    .request(new HomaRequestBusinessRecheckRequest(FOUNDER, "no-such-provider", "Safety report")))
                    .isInstanceOf(ProviderNotFoundException.class);
            assertThatThrownBy(() -> new HomaRequestMemberRecheck(members, kyc, auditLog, clock)
                    .request(new HomaRequestMemberRecheckRequest(FOUNDER, "no-such-member", "Safety report")))
                    .isInstanceOf(TeamMemberNotFoundException.class);
            assertThat(kyc.opened).isEmpty();
            assertThat(auditLog.all).isEmpty();
        }
    }
}
