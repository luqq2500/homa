package verification.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static verification.support.VerificationFixtures.NEXT_YEAR;
import static verification.support.VerificationFixtures.NOW;

import ie.events.BusinessFlagClearedIE;
import ie.events.BusinessKybRejectedIE;
import ie.events.BusinessLapsedIE;
import ie.events.BusinessSeriouslyFlaggedIE;
import ie.events.BusinessVerifiedIE;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import verification.domain.BusinessStatus;
import verification.support.VerificationWorld;
import verification.usecase.dto.ReportBusinessFlagClearedRequest;
import verification.usecase.dto.ReportBusinessLapseRequest;
import verification.usecase.dto.ReportBusinessResultRequest;
import verification.usecase.dto.ReportBusinessReverificationNeededRequest;
import verification.usecase.dto.ReportBusinessSeriousFlagRequest;
import verification.usecase.dto.RequestEarlyRecheckRequest;
import verification.usecase.dto.StartBusinessCheckRequest;
import verification.usecase.exception.BusinessNotFoundException;

class BusinessVerificationUsecasesTest {

    private static final String BUSINESS = "business-1";

    private final VerificationWorld world = new VerificationWorld();
    private final KycReportBusinessResult results =
            new KycReportBusinessResult(world.businesses, world.publisher, world.clock);

    @Test
    void aRegisteredBusinessStartsUnverifiedAndIsRegisteredOnce() {
        world.registeredBusiness(BUSINESS);
        world.registeredBusiness(BUSINESS);

        assertThat(world.businesses.find(BUSINESS).orElseThrow().status()).isEqualTo(BusinessStatus.NOT_VERIFIED);
        assertThat(world.businesses.timesSaved(BUSINESS)).isEqualTo(1);
    }

    @Test
    void theProviderStartsTheKybOnTheKycProvidersPages() {
        world.registeredBusiness(BUSINESS);
        ProviderStartBusinessCheck usecase = new ProviderStartBusinessCheck(world.businesses, world.kyc);

        assertThat(usecase.start(new StartBusinessCheckRequest(BUSINESS)).session()).isEqualTo("session-for-business-1");
        assertThat(world.kycCalls).containsExactly("business-check business-1");
        assertThatThrownBy(() -> usecase.start(new StartBusinessCheckRequest("none")))
                .isInstanceOf(BusinessNotFoundException.class);
    }

    @Test
    void aPassedKybVerifiesTheBusinessAndTellsTheOtherContexts() {
        world.registeredBusiness(BUSINESS);

        String status = results.report(
                new ReportBusinessResultRequest(BUSINESS, true, "202401012345", "Bersih Sdn Bhd", NEXT_YEAR))
                .businessStatus();

        assertThat(status).isEqualTo("VERIFIED");
        assertThat(world.businesses.timesSaved(BUSINESS)).isEqualTo(2);
        assertThat(world.publisher.published()).containsExactly(
                new BusinessVerifiedIE(BUSINESS, "202401012345", "Bersih Sdn Bhd", NEXT_YEAR, NOW, NOW));
    }

    @Test
    void aRejectedKybTellsTheBusinessToRedoIt() {
        world.registeredBusiness(BUSINESS);

        String status = results.report(new ReportBusinessResultRequest(BUSINESS, false, null, null, null))
                .businessStatus();

        assertThat(status).isEqualTo("REJECTED");
        assertThat(world.businesses.timesSaved(BUSINESS)).isEqualTo(2);
        assertThat(world.publisher.published()).containsExactly(new BusinessKybRejectedIE(BUSINESS, NOW));
    }

    @Test
    void aFailedReverificationChangesNothingAndTellsNoOne() {
        world.verifiedBusiness(BUSINESS);

        String status = results.report(new ReportBusinessResultRequest(BUSINESS, false, null, null, null))
                .businessStatus();

        assertThat(status).isEqualTo("VERIFIED");
        assertThat(world.businesses.timesSaved(BUSINESS)).isEqualTo(2);
        assertThat(world.publisher.published(BusinessKybRejectedIE.class)).isEmpty();
    }

    @Test
    void reverificationNeededSoonerMovesVerifiedUntilAndTellsTheOtherContexts() {
        world.verifiedBusiness(BUSINESS);
        Instant sooner = NOW.plus(Duration.ofDays(14));
        KycReportBusinessReverificationNeeded usecase =
                new KycReportBusinessReverificationNeeded(world.businesses, world.publisher, world.clock);

        assertThat(usecase.report(new ReportBusinessReverificationNeededRequest(BUSINESS, sooner)).verifiedUntil())
                .isEqualTo(sooner);
        assertThat(usecase.report(new ReportBusinessReverificationNeededRequest(BUSINESS, NEXT_YEAR)).verifiedUntil())
                .isEqualTo(sooner);

        assertThat(world.businesses.timesSaved(BUSINESS)).isEqualTo(3);
        assertThat(world.publisher.published(BusinessVerifiedIE.class)).hasSize(2).last()
                .isEqualTo(new BusinessVerifiedIE(BUSINESS, "202401012345", "Bersih Sdn Bhd", sooner, NOW, NOW));
    }

    @Test
    void aMissedReverificationLapsesTheBusinessOnce() {
        world.verifiedBusiness(BUSINESS);
        KycReportBusinessLapse usecase = new KycReportBusinessLapse(world.businesses, world.publisher, world.clock);

        assertThat(usecase.report(new ReportBusinessLapseRequest(BUSINESS)).businessStatus()).isEqualTo("LAPSED");
        assertThat(usecase.report(new ReportBusinessLapseRequest(BUSINESS)).businessStatus()).isEqualTo("LAPSED");

        assertThat(world.businesses.timesSaved(BUSINESS)).isEqualTo(3);
        assertThat(world.publisher.published(BusinessLapsedIE.class)).containsExactly(new BusinessLapsedIE(BUSINESS, NOW));
    }

    @Test
    void aSeriousFlagIsRecordedAndAnnouncedOnceAndCanBeCleared() {
        world.verifiedBusiness(BUSINESS);
        KycReportBusinessSeriousFlag flagger =
                new KycReportBusinessSeriousFlag(world.businesses, world.publisher, world.clock);
        KycReportBusinessFlagCleared clearer =
                new KycReportBusinessFlagCleared(world.businesses, world.publisher, world.clock);

        assertThat(flagger.report(new ReportBusinessSeriousFlagRequest(BUSINESS)).businessStatus())
                .isEqualTo("SERIOUSLY_FLAGGED");
        flagger.report(new ReportBusinessSeriousFlagRequest(BUSINESS));
        assertThat(world.businesses.timesSaved(BUSINESS)).isEqualTo(3);
        assertThat(world.publisher.published(BusinessSeriouslyFlaggedIE.class))
                .containsExactly(new BusinessSeriouslyFlaggedIE(BUSINESS, NOW));

        assertThat(clearer.report(new ReportBusinessFlagClearedRequest(BUSINESS)).businessStatus()).isEqualTo("VERIFIED");
        clearer.report(new ReportBusinessFlagClearedRequest(BUSINESS));
        assertThat(world.businesses.timesSaved(BUSINESS)).isEqualTo(4);
        assertThat(world.publisher.published(BusinessFlagClearedIE.class))
                .containsExactly(new BusinessFlagClearedIE(BUSINESS, true, NOW));
    }

    @Test
    void anAdminAsksTheKycProviderForAnEarlyRecheckWithAReason() {
        world.verifiedBusiness(BUSINESS);
        String memberId = world.teamMember(BUSINESS, "HELPER");
        AdminRequestEarlyRecheck usecase = new AdminRequestEarlyRecheck(world.businesses, world.members,
                world.rechecks, world.kyc, world.clock);
        world.kycCalls.clear();

        String forBusiness = usecase.request(new RequestEarlyRecheckRequest("admin-1", BUSINESS, "Safety report")).recheckId();
        usecase.request(new RequestEarlyRecheckRequest("admin-1", memberId, "Damage report"));

        assertThat(world.rechecks.find(forBusiness)).get().satisfies(recheck -> {
            assertThat(recheck.subjectId()).isEqualTo(BUSINESS);
            assertThat(recheck.adminId()).isEqualTo("admin-1");
            assertThat(recheck.requestedAt()).isEqualTo(NOW);
        });
        assertThat(world.kycCalls).containsExactly("early-recheck business-1 Safety report",
                "early-recheck " + memberId + " Damage report");
        assertThatThrownBy(() -> usecase.request(new RequestEarlyRecheckRequest("admin-1", "nobody", "Why")))
                .isInstanceOf(BusinessNotFoundException.class);
        assertThat(world.rechecks.all()).hasSize(2);
    }
}
