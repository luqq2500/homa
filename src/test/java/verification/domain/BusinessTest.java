package verification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static verification.support.VerificationFixtures.BUSINESS_ID;
import static verification.support.VerificationFixtures.NEXT_YEAR;
import static verification.support.VerificationFixtures.NOW;
import static verification.support.VerificationFixtures.verifiedBusiness;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import verification.domain.exception.BusinessNotVerifiedException;
import verification.domain.exception.BusinessSeriouslyFlaggedException;

class BusinessTest {

    @Test
    void aRegisteredBusinessStartsUnverified() {
        Business business = Business.register(BUSINESS_ID);

        assertThat(business.id()).isEqualTo(BUSINESS_ID);
        assertThat(business.status()).isEqualTo(BusinessStatus.NOT_VERIFIED);
        assertThat(business.isVerified()).isFalse();
        assertThat(business.verifiedUntil()).isNull();
        assertThatThrownBy(business::requireVerified).isInstanceOf(BusinessNotVerifiedException.class);
    }

    @Test
    void aPassedKybRecordsWhatHomaUsesFromTheResult() {
        Business business = verifiedBusiness();

        assertThat(business.status()).isEqualTo(BusinessStatus.VERIFIED);
        assertThat(business.isVerified()).isTrue();
        assertThat(business.ssmNumber()).isEqualTo("202401012345");
        assertThat(business.registeredName()).isEqualTo("Bersih Sdn Bhd");
        assertThat(business.verifiedUntil()).isEqualTo(NEXT_YEAR);
        assertThat(business.lastVerifiedAt()).isEqualTo(NOW);
        business.requireVerified();
    }

    @Test
    void aRejectedBusinessIsToldToRedoItAndCanPassLater() {
        Business business = Business.register(BUSINESS_ID);

        assertThat(business.failed()).isTrue();
        assertThat(business.status()).isEqualTo(BusinessStatus.REJECTED);
        assertThat(business.failed()).isTrue();

        business.passed("202401012345", "Bersih Sdn Bhd", NEXT_YEAR, NOW);
        assertThat(business.status()).isEqualTo(BusinessStatus.VERIFIED);
    }

    @Test
    void aFailedReverificationChangesNothing() {
        Business business = verifiedBusiness();

        assertThat(business.failed()).isFalse();

        assertThat(business.status()).isEqualTo(BusinessStatus.VERIFIED);
        assertThat(business.verifiedUntil()).isEqualTo(NEXT_YEAR);

        business.lapse();
        assertThat(business.failed()).isFalse();
        assertThat(business.status()).isEqualTo(BusinessStatus.LAPSED);
    }

    @Test
    void aPassedReverificationMovesTheVerifiedUntilDateOut() {
        Business business = verifiedBusiness();
        Instant later = NOW.plus(Duration.ofDays(300));
        Instant twoYears = NEXT_YEAR.plus(Duration.ofDays(365));

        business.passed("202401012345", "Bersih Services Sdn Bhd", twoYears, later);

        assertThat(business.verifiedUntil()).isEqualTo(twoYears);
        assertThat(business.lastVerifiedAt()).isEqualTo(later);
        assertThat(business.registeredName()).isEqualTo("Bersih Services Sdn Bhd");
    }

    @Test
    void reverificationNeededByASoonerDateBringsVerifiedUntilForward() {
        Business business = verifiedBusiness();
        Instant sooner = NOW.plus(Duration.ofDays(14));

        assertThat(business.reverificationNeededBy(sooner)).isTrue();

        assertThat(business.verifiedUntil()).isEqualTo(sooner);
        assertThat(business.status()).isEqualTo(BusinessStatus.VERIFIED);
    }

    @Test
    void reverificationNeededByALaterOrTheSameDateChangesNothing() {
        Business business = verifiedBusiness();

        assertThat(business.reverificationNeededBy(NEXT_YEAR.plusSeconds(1))).isFalse();
        assertThat(business.reverificationNeededBy(NEXT_YEAR)).isFalse();
        assertThat(business.verifiedUntil()).isEqualTo(NEXT_YEAR);
        assertThat(Business.register(BUSINESS_ID).reverificationNeededBy(NOW)).isFalse();
    }

    @Test
    void aBusinessThatMissesItsReverificationLapses() {
        Business business = verifiedBusiness();

        assertThat(business.lapse()).isTrue();

        assertThat(business.status()).isEqualTo(BusinessStatus.LAPSED);
        assertThat(business.isVerified()).isFalse();
        assertThatThrownBy(business::requireVerified).isInstanceOf(BusinessNotVerifiedException.class);
        assertThat(business.lapse()).isFalse();
    }

    @Test
    void aLapsedBusinessIsVerifiedAgainOnceItPasses() {
        Business business = verifiedBusiness();
        business.lapse();

        business.passed("202401012345", "Bersih Sdn Bhd", NEXT_YEAR.plusSeconds(60), NOW.plusSeconds(60));

        assertThat(business.status()).isEqualTo(BusinessStatus.VERIFIED);
    }

    @Test
    void onlyAVerifiedBusinessCanLapse() {
        Business business = Business.register(BUSINESS_ID);

        assertThat(business.lapse()).isFalse();
        assertThat(business.status()).isEqualTo(BusinessStatus.NOT_VERIFIED);
    }

    @Test
    void aSeriousFlagLapsesTheBusinessAtOnce() {
        Business business = verifiedBusiness();

        assertThat(business.flagSeriously()).isTrue();

        assertThat(business.status()).isEqualTo(BusinessStatus.SERIOUSLY_FLAGGED);
        assertThat(business.statusBeforeFlag()).contains(BusinessStatus.VERIFIED);
        assertThat(business.isVerified()).isFalse();
        assertThat(business.flagSeriously()).isFalse();
        assertThat(business.statusBeforeFlag()).contains(BusinessStatus.VERIFIED);
    }

    @Test
    void aSeriouslyFlaggedBusinessCannotPassUntilTheFlagIsCleared() {
        Business business = verifiedBusiness();
        business.flagSeriously();

        assertThatThrownBy(() -> business.passed("202401012345", "Bersih Sdn Bhd", NEXT_YEAR, NOW))
                .isInstanceOf(BusinessSeriouslyFlaggedException.class);
        assertThat(business.status()).isEqualTo(BusinessStatus.SERIOUSLY_FLAGGED);
        assertThat(business.failed()).isFalse();
    }

    @Test
    void clearingTheFlagPutsTheBusinessBackWhereItStood() {
        Business verified = verifiedBusiness();
        verified.flagSeriously();
        Business lapsed = verifiedBusiness();
        lapsed.lapse();
        lapsed.flagSeriously();

        assertThat(verified.clearFlag()).isTrue();
        assertThat(lapsed.clearFlag()).isTrue();

        assertThat(verified.status()).isEqualTo(BusinessStatus.VERIFIED);
        assertThat(verified.statusBeforeFlag()).isEmpty();
        assertThat(lapsed.status()).isEqualTo(BusinessStatus.LAPSED);
        assertThat(verified.clearFlag()).isFalse();
        assertThat(verified.status()).isEqualTo(BusinessStatus.VERIFIED);
    }

    @Test
    void aStoredBusinessKeepsItsState() {
        Business stored = new Business(BUSINESS_ID, BusinessStatus.SERIOUSLY_FLAGGED, BusinessStatus.LAPSED,
                "202401012345", "Bersih Sdn Bhd", NEXT_YEAR, NOW);

        assertThat(stored.clearFlag()).isTrue();
        assertThat(stored.status()).isEqualTo(BusinessStatus.LAPSED);
        assertThat(stored.ssmNumber()).isEqualTo("202401012345");
    }
}
