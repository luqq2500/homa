package policy.domain;

import static policy.domain.PolicyBuilders.A_WEEK_LATER;
import static policy.domain.PolicyBuilders.LAUNCH;
import static policy.domain.PolicyBuilders.theLaunchPolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kernel.Percentage;
import policy.domain.PolicyValue.CancellationTierFee;
import policy.domain.PolicyValue.CheckInPeriod;
import policy.domain.PolicyValue.CustomerNoShowFee;
import policy.domain.PolicyValue.DurationRange;
import policy.domain.PolicyValue.ProviderLateCancellationFine;
import policy.domain.PolicyValue.ProviderNoShowFine;
import policy.domain.PolicyValue.ServiceFee;
import policy.domain.exception.CancellationFeeAboveNoShowCeilingException;
import policy.domain.exception.FlexibleTierMustBeFreeException;
import policy.domain.exception.NoShowFineBelowLateCancellationFineException;
import policy.domain.exception.PolicyChangeBeforeCurrentVersionException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Feature: Homa-set values. */
class HomaPolicyVersioningTest {

    @Nested
    class TheLaunchValues {

        private final PolicySnapshot launch = theLaunchPolicy().snapshot();

        @Test
        void theFirstVersionHoldsTheLaunchValues() {
            assertThat(launch.version()).isEqualTo(1);
            assertThat(launch.effectiveFrom()).isEqualTo(LAUNCH);
            assertThat(launch.serviceFee()).isEqualTo(Percentage.of(15));
            assertThat(launch.checkInRadiusMetres()).isEqualTo(300);
            assertThat(launch.fineDeductionCap()).isEqualTo(Percentage.of(50));
        }

        @Test
        void bookingsAreOneToSixHoursStartingTwoHoursToThirtyDaysAhead() {
            assertThat(launch.durationRange()).isEqualTo(new DurationRange(1, 6));
            assertThat(launch.minimumNotice()).isEqualTo(Duration.ofHours(2));
            assertThat(launch.bookingHorizon()).isEqualTo(Duration.ofDays(30));
        }

        @Test
        void requestsExpireAfterADayAndTheFeeWindowIsTheDayBeforeTheStart() {
            assertThat(launch.requestExpiry()).isEqualTo(Duration.ofHours(24));
            assertThat(launch.feeWindow()).isEqualTo(Duration.ofHours(24));
        }

        @Test
        void tiersChargeNothingTenOrTwentyPercentOfTheHoursFee() {
            assertThat(launch.cancellationFee(CancellationTier.FLEXIBLE)).isEqualTo(Percentage.ZERO);
            assertThat(launch.cancellationFee(CancellationTier.STANDARD)).isEqualTo(Percentage.of(10));
            assertThat(launch.cancellationFee(CancellationTier.FIRM)).isEqualTo(Percentage.of(20));
        }

        @Test
        void providersAreFinedTwentyPercentForLateCancellationAndThirtyForANoShow() {
            assertThat(launch.providerLateCancellationFine()).isEqualTo(Percentage.of(20));
            assertThat(launch.providerNoShowFine()).isEqualTo(Percentage.of(30));
        }

        @Test
        void theProviderIsRemindedToNameTheTeamSixHoursAhead() {
            assertThat(launch.teamReminder()).isEqualTo(Duration.ofHours(6));
        }

        @Test
        void checkInOpensFifteenMinutesBeforeTheStartAndClosesFifteenMinutesAfter() {
            assertThat(launch.earliestCheckIn()).isEqualTo(Duration.ofMinutes(15));
            assertThat(launch.checkInPeriod()).isEqualTo(Duration.ofMinutes(15));
        }

        @Test
        void aCustomerNoShowCostsTheTravelFeePlusThirtyPercentAfterAFifteenMinuteWait() {
            assertThat(launch.customerNoShowWait()).isEqualTo(Duration.ofMinutes(15));
            assertThat(launch.customerNoShowFee()).isEqualTo(Percentage.of(30));
            assertThat(launch.noShowReportWindow()).isEqualTo(Duration.ofHours(1));
        }

        @Test
        void completionIsRemindedAfterHalfAnHourAndDueWithinADay() {
            assertThat(launch.completionReminder()).isEqualTo(Duration.ofMinutes(30));
            assertThat(launch.completionDeadline()).isEqualTo(Duration.ofHours(24));
            assertThat(launch.disputeWindowExtension()).isEqualTo(Duration.ofHours(1));
            assertThat(launch.evidenceRetention()).isEqualTo(Duration.ofDays(7));
        }

        @Test
        void customersRateWithinFourteenDaysAndAveragesShowAfterThreeRatings() {
            assertThat(launch.ratingWindow()).isEqualTo(Duration.ofDays(14));
            assertThat(launch.minimumRatingsToShow()).isEqualTo(3);
        }

        @Test
        void everyoneIsReverifiedEveryThreeMonthsWithAFourteenDayReminder() {
            assertThat(launch.reverificationPeriodMonths()).isEqualTo(3);
            assertThat(launch.reverificationReminder()).isEqualTo(Duration.ofDays(14));
            assertThat(launch.earlyRecheckDeadline()).isEqualTo(Duration.ofDays(14));
        }

        @Test
        void eachPersonGetsThreeFaceCheckAttempts() {
            assertThat(launch.faceCheckAttempts()).isEqualTo(3);
        }

        @Test
        void providersAndIndependentMembersUseMalaysianDocumentsAndBusinessMembersAnyCountry() {
            assertThat(launch.acceptedDocuments()).containsExactlyInAnyOrder(
                    AcceptedDocument.issuedBy(DocumentHolder.PROVIDER, DocumentType.IDENTITY_CARD, "MY"),
                    AcceptedDocument.issuedBy(DocumentHolder.PROVIDER, DocumentType.PASSPORT, "MY"),
                    AcceptedDocument.issuedBy(DocumentHolder.INDEPENDENT_TEAM_MEMBER, DocumentType.IDENTITY_CARD, "MY"),
                    AcceptedDocument.issuedBy(DocumentHolder.INDEPENDENT_TEAM_MEMBER, DocumentType.PASSPORT, "MY"),
                    AcceptedDocument.fromAnyCountry(DocumentHolder.BUSINESS_TEAM_MEMBER, DocumentType.IDENTITY_CARD),
                    AcceptedDocument.fromAnyCountry(DocumentHolder.BUSINESS_TEAM_MEMBER, DocumentType.PASSPORT));
        }

        @Test
        void everyHomaSetValueIsInTheFirstVersion() {
            assertThat(launch.values()).hasSize(30);
            assertThatThrownBy(() -> launch.values().clear()).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void tiersStartWithTheirLaunchFees() {
            assertThat(CancellationTier.FLEXIBLE.launchFee()).isEqualTo(Percentage.ZERO);
            assertThat(CancellationTier.STANDARD.launchFee()).isEqualTo(Percentage.of(10));
            assertThat(CancellationTier.FIRM.launchFee()).isEqualTo(Percentage.of(20));
        }
    }

    @Nested
    class ChangingAValue {

        @Test
        void changingAValueCreatesANewVersionAndKeepsTheOldOne() {
            HomaPolicy version1 = theLaunchPolicy();
            PolicySnapshot snapshotFromVersion1 = version1.snapshot();

            HomaPolicy version2 = version1.change(new CheckInPeriod(Duration.ofMinutes(20)), A_WEEK_LATER);

            assertThat(version2.version()).isEqualTo(2);
            assertThat(version2.snapshot().checkInPeriod()).isEqualTo(Duration.ofMinutes(20));
            assertThat(version2.snapshot().effectiveFrom()).isEqualTo(A_WEEK_LATER);
            assertThat(snapshotFromVersion1.checkInPeriod()).isEqualTo(Duration.ofMinutes(15));
            assertThat(version1.version()).isEqualTo(1);
            assertThat(version1.snapshot().checkInPeriod()).isEqualTo(Duration.ofMinutes(15));
        }

        @Test
        void aChangeLeavesEveryOtherValueAsItWas() {
            HomaPolicy version2 = theLaunchPolicy().change(new ServiceFee(Percentage.of(12)), A_WEEK_LATER);

            assertThat(version2.snapshot().serviceFee()).isEqualTo(Percentage.of(12));
            assertThat(version2.snapshot().checkInPeriod()).isEqualTo(Duration.ofMinutes(15));
            assertThat(version2.snapshot().values()).hasSize(30);
        }

        @Test
        void changingOneTierLeavesTheOtherTiersAsTheyWere() {
            HomaPolicy version2 = theLaunchPolicy()
                    .change(new CancellationTierFee(CancellationTier.STANDARD, Percentage.of(15)), A_WEEK_LATER);

            assertThat(version2.snapshot().cancellationFee(CancellationTier.STANDARD)).isEqualTo(Percentage.of(15));
            assertThat(version2.snapshot().cancellationFee(CancellationTier.FIRM)).isEqualTo(Percentage.of(20));
            assertThat(version2.snapshot().cancellationFee(CancellationTier.FLEXIBLE)).isEqualTo(Percentage.ZERO);
        }

        @Test
        void versionsCountUpWithEachChange() {
            HomaPolicy version3 = theLaunchPolicy()
                    .change(new CheckInPeriod(Duration.ofMinutes(20)), A_WEEK_LATER)
                    .change(new CheckInPeriod(Duration.ofMinutes(25)), A_WEEK_LATER);

            assertThat(version3.version()).isEqualTo(3);
            assertThat(version3.snapshot().version()).isEqualTo(3);
        }

        @Test
        void aChangeCannotTakeEffectBeforeTheVersionInForce() {
            HomaPolicy policy = theLaunchPolicy();
            Instant aSecondBeforeLaunch = LAUNCH.minusSeconds(1);

            assertThatThrownBy(() -> policy.change(new CheckInPeriod(Duration.ofMinutes(20)), aSecondBeforeLaunch))
                    .isInstanceOf(PolicyChangeBeforeCurrentVersionException.class);
            assertThat(policy.change(new CheckInPeriod(Duration.ofMinutes(20)), LAUNCH).version()).isEqualTo(2);
        }

        @Test
        void theFirstVersionNeedsATimeToTakeEffect() {
            assertThatThrownBy(() -> HomaPolicy.initial(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class TheNoShowCeiling {

        @Test
        void aCancellationFeeCanNeverExceedTheCustomerNoShowFee() {
            HomaPolicy policy = theLaunchPolicy();

            assertThatThrownBy(() -> policy.change(firmTier(Percentage.of(40)), A_WEEK_LATER))
                    .isInstanceOf(CancellationFeeAboveNoShowCeilingException.class)
                    .hasMessageContaining("FIRM");
            assertThat(policy.version()).isEqualTo(1);
        }

        @Test
        void aCancellationFeeCanEqualTheCustomerNoShowFee() {
            HomaPolicy changed = theLaunchPolicy().change(firmTier(Percentage.of(30)), A_WEEK_LATER);

            assertThat(changed.snapshot().cancellationFee(CancellationTier.FIRM)).isEqualTo(Percentage.of(30));
        }

        @Test
        void aCancellationFeeOneBasisPointAboveTheCustomerNoShowFeeIsRejected() {
            assertThatThrownBy(() -> theLaunchPolicy().change(firmTier(Percentage.ofBasisPoints(3001)), A_WEEK_LATER))
                    .isInstanceOf(CancellationFeeAboveNoShowCeilingException.class);
        }

        @Test
        void theStandardTierIsHeldToTheSameCeiling() {
            assertThatThrownBy(() -> theLaunchPolicy()
                            .change(new CancellationTierFee(CancellationTier.STANDARD, Percentage.of(31)), A_WEEK_LATER))
                    .isInstanceOf(CancellationFeeAboveNoShowCeilingException.class)
                    .hasMessageContaining("STANDARD");
        }

        @Test
        void theCustomerNoShowFeeCannotDropBelowATierFee() {
            HomaPolicy policy = theLaunchPolicy();

            assertThatThrownBy(() -> policy.change(new CustomerNoShowFee(Percentage.of(19)), A_WEEK_LATER))
                    .isInstanceOf(CancellationFeeAboveNoShowCeilingException.class);
            assertThat(policy.change(new CustomerNoShowFee(Percentage.of(20)), A_WEEK_LATER).snapshot().customerNoShowFee())
                    .isEqualTo(Percentage.of(20));
        }

        @Test
        void theFlexibleTierIsAlwaysFree() {
            assertThatThrownBy(() -> theLaunchPolicy()
                            .change(new CancellationTierFee(CancellationTier.FLEXIBLE, Percentage.ofBasisPoints(1)), A_WEEK_LATER))
                    .isInstanceOf(FlexibleTierMustBeFreeException.class);
            assertThat(theLaunchPolicy()
                            .change(new CancellationTierFee(CancellationTier.FLEXIBLE, Percentage.ZERO), A_WEEK_LATER)
                            .version())
                    .isEqualTo(2);
        }

        private CancellationTierFee firmTier(Percentage fee) {
            return new CancellationTierFee(CancellationTier.FIRM, fee);
        }
    }

    @Nested
    class ProviderFines {

        @Test
        void notShowingUpIsNeverCheaperThanCancellingLate() {
            assertThatThrownBy(() -> theLaunchPolicy().change(new ProviderLateCancellationFine(Percentage.of(31)), A_WEEK_LATER))
                    .isInstanceOf(NoShowFineBelowLateCancellationFineException.class);
            assertThatThrownBy(() -> theLaunchPolicy().change(new ProviderNoShowFine(Percentage.of(19)), A_WEEK_LATER))
                    .isInstanceOf(NoShowFineBelowLateCancellationFineException.class);
        }

        @Test
        void theLateCancellationFineCanEqualTheNoShowFine() {
            HomaPolicy changed = theLaunchPolicy().change(new ProviderLateCancellationFine(Percentage.of(30)), A_WEEK_LATER);

            assertThat(changed.snapshot().providerLateCancellationFine()).isEqualTo(Percentage.of(30));
        }
    }
}
