package policy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static policy.domain.HomaValue.*;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import kernel.Percentage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import policy.domain.exception.HomaValueOutOfRangeException;
import policy.domain.exception.HomaValueRuleBrokenException;
import policy.domain.exception.HomaValueUnchangedException;

class HomaValuesTest {

    private final HomaValues launch = HomaValues.launch();

    @Test
    void homaLaunchesWithTheValuesInTheProductDescription() {
        assertThat(launch.version()).isEqualTo(1);
        assertThat(launch.percentageOf(SERVICE_FEE)).isEqualTo(Percentage.of(15));
        assertThat(launch.amountOf(MINIMUM_HOURS)).isEqualTo(1);
        assertThat(launch.amountOf(MAXIMUM_HOURS)).isEqualTo(6);
        assertThat(launch.durationOf(MINIMUM_NOTICE)).isEqualTo(Duration.ofHours(2));
        assertThat(launch.durationOf(BOOKING_HORIZON)).isEqualTo(Duration.ofDays(30));
        assertThat(launch.durationOf(TEAM_REMINDER_TIME)).isEqualTo(Duration.ofHours(6));
        assertThat(launch.durationOf(EARLIEST_CHECK_IN)).isEqualTo(Duration.ofMinutes(15));
        assertThat(launch.durationOf(CHECK_IN_PERIOD)).isEqualTo(Duration.ofMinutes(15));
        assertThat(launch.amountOf(CHECK_IN_RADIUS)).isEqualTo(300);
        assertThat(launch.durationOf(CUSTOMER_NO_SHOW_WAIT)).isEqualTo(Duration.ofMinutes(15));
        assertThat(launch.percentageOf(CUSTOMER_NO_SHOW_FEE)).isEqualTo(Percentage.of(30));
        assertThat(launch.durationOf(NO_SHOW_REPORT_WINDOW)).isEqualTo(Duration.ofHours(1));
        assertThat(launch.durationOf(REQUEST_EXPIRY_TIME)).isEqualTo(Duration.ofHours(24));
        assertThat(launch.durationOf(RESPONSE_DEADLINE)).isEqualTo(Duration.ofHours(1));
        assertThat(launch.durationOf(COMPLETION_REMINDER_TIME)).isEqualTo(Duration.ofMinutes(30));
        assertThat(launch.durationOf(COMPLETION_DEADLINE)).isEqualTo(Duration.ofHours(24));
        assertThat(launch.durationOf(EVIDENCE_RETENTION_PERIOD)).isEqualTo(Duration.ofDays(120));
        assertThat(launch.durationOf(DISPUTE_WINDOW_EXTENSION)).isEqualTo(Duration.ofHours(24));
        assertThat(launch.durationOf(FEE_WINDOW)).isEqualTo(Duration.ofHours(24));
        assertThat(launch.percentageOf(PROVIDER_LATE_CANCELLATION_FINE)).isEqualTo(Percentage.of(20));
        assertThat(launch.percentageOf(PROVIDER_NO_SHOW_FINE)).isEqualTo(Percentage.of(30));
        assertThat(launch.percentageOf(FINE_DEDUCTION_CAP)).isEqualTo(Percentage.of(50));
        assertThat(launch.durationOf(RATING_WINDOW)).isEqualTo(Duration.ofDays(14));
        assertThat(launch.amountOf(MINIMUM_RATINGS_TO_SHOW)).isEqualTo(3);
    }

    @Test
    void flexibleIsFreeAndStandardAndFirmChargeTheirShareOfTheHoursFee() {
        assertThat(launch.cancellationFeeOf(CancellationTier.FLEXIBLE)).isEqualTo(Percentage.ZERO);
        assertThat(launch.cancellationFeeOf(CancellationTier.STANDARD)).isEqualTo(Percentage.of(10));
        assertThat(launch.cancellationFeeOf(CancellationTier.FIRM)).isEqualTo(Percentage.of(20));
    }

    @Test
    void changingAValueMakesTheNextVersionAndLeavesTheOldOneAsItWas() {
        HomaValues changed = launch.changed(SERVICE_FEE, 12);

        assertThat(changed.version()).isEqualTo(2);
        assertThat(changed.percentageOf(SERVICE_FEE)).isEqualTo(Percentage.of(12));
        assertThat(changed.amountOf(FEE_WINDOW)).isEqualTo(launch.amountOf(FEE_WINDOW));
        assertThat(launch.percentageOf(SERVICE_FEE)).isEqualTo(Percentage.of(15));
    }

    @Test
    void aChangeMustChangeTheValue() {
        assertThatThrownBy(() -> launch.changed(SERVICE_FEE, 15)).isInstanceOf(HomaValueUnchangedException.class);
    }

    @Test
    void aPercentageIsFromZeroToAHundred() {
        assertThat(launch.changed(SERVICE_FEE, 0).amountOf(SERVICE_FEE)).isZero();
        assertThat(launch.changed(SERVICE_FEE, 100).amountOf(SERVICE_FEE)).isEqualTo(100);
        assertThatThrownBy(() -> launch.changed(SERVICE_FEE, -1)).isInstanceOf(HomaValueOutOfRangeException.class);
        assertThatThrownBy(() -> launch.changed(SERVICE_FEE, 101)).isInstanceOf(HomaValueOutOfRangeException.class);
    }

    @ParameterizedTest
    @EnumSource(value = HomaValue.class, names = {"CHECK_IN_PERIOD", "CHECK_IN_RADIUS", "MINIMUM_RATINGS_TO_SHOW", "MINIMUM_HOURS"})
    void timesDistancesAndCountsAreAtLeastOne(HomaValue value) {
        assertThatThrownBy(() -> launch.changed(value, 0)).isInstanceOf(HomaValueOutOfRangeException.class);
    }

    @Test
    void aTimeOfOneMinuteIsAllowed() {
        assertThat(launch.changed(CHECK_IN_PERIOD, 1).durationOf(CHECK_IN_PERIOD)).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void theMinimumHoursCannotBeMoreThanTheMaximumHours() {
        assertThat(launch.changed(MINIMUM_HOURS, 6).amountOf(MINIMUM_HOURS)).isEqualTo(6);
        assertThatThrownBy(() -> launch.changed(MINIMUM_HOURS, 7)).isInstanceOf(HomaValueRuleBrokenException.class);
        assertThatThrownBy(() -> launch.changed(MAXIMUM_HOURS, 0)).isInstanceOf(HomaValueOutOfRangeException.class);
    }

    @Test
    void theResponseDeadlineIsAlwaysShorterThanTheMinimumNotice() {
        assertThat(launch.changed(RESPONSE_DEADLINE, 119).amountOf(RESPONSE_DEADLINE)).isEqualTo(119);
        assertThatThrownBy(() -> launch.changed(RESPONSE_DEADLINE, 120)).isInstanceOf(HomaValueRuleBrokenException.class);
        assertThatThrownBy(() -> launch.changed(MINIMUM_NOTICE, 60)).isInstanceOf(HomaValueRuleBrokenException.class);
    }

    @Test
    void noCancellationFeeIsHigherThanTheCustomerNoShowFee() {
        assertThat(launch.changed(STANDARD_CANCELLATION_FEE, 30).amountOf(STANDARD_CANCELLATION_FEE)).isEqualTo(30);
        assertThat(launch.changed(FIRM_CANCELLATION_FEE, 30).amountOf(FIRM_CANCELLATION_FEE)).isEqualTo(30);
        assertThatThrownBy(() -> launch.changed(STANDARD_CANCELLATION_FEE, 31)).isInstanceOf(HomaValueRuleBrokenException.class);
        assertThatThrownBy(() -> launch.changed(FIRM_CANCELLATION_FEE, 31)).isInstanceOf(HomaValueRuleBrokenException.class);
        assertThatThrownBy(() -> launch.changed(CUSTOMER_NO_SHOW_FEE, 19)).isInstanceOf(HomaValueRuleBrokenException.class);
    }

    @Test
    void theProviderNoShowFineIsNeverLowerThanTheLateCancellationFine() {
        assertThat(launch.changed(PROVIDER_LATE_CANCELLATION_FINE, 30).amountOf(PROVIDER_LATE_CANCELLATION_FINE)).isEqualTo(30);
        assertThatThrownBy(() -> launch.changed(PROVIDER_LATE_CANCELLATION_FINE, 31)).isInstanceOf(HomaValueRuleBrokenException.class);
        assertThatThrownBy(() -> launch.changed(PROVIDER_NO_SHOW_FINE, 19)).isInstanceOf(HomaValueRuleBrokenException.class);
    }

    @Test
    void aStoredVersionNeedsAnAmountForEveryValue() {
        Map<HomaValue, Long> amounts = new EnumMap<>(HomaValue.class);
        for (HomaValue value : HomaValue.values()) {
            amounts.put(value, value.launchAmount());
        }
        assertThat(HomaValues.of(1, amounts)).isEqualTo(launch).hasSameHashCodeAs(launch);

        amounts.remove(RATING_WINDOW);
        assertThatThrownBy(() -> HomaValues.of(1, amounts)).isInstanceOf(HomaValueOutOfRangeException.class);
    }

    @Test
    void versionsAreTheSameOnlyWithTheSameNumberAndAmounts() {
        HomaValues changed = launch.changed(SERVICE_FEE, 12);
        Map<HomaValue, Long> amounts = new EnumMap<>(HomaValue.class);
        for (HomaValue value : HomaValue.values()) {
            amounts.put(value, changed.amountOf(value));
        }
        assertThat(HomaValues.of(1, amounts)).isNotEqualTo(launch);
        assertThat(HomaValues.of(2, amounts)).isEqualTo(changed);
        assertThat(HomaValues.of(3, amounts)).isNotEqualTo(changed);
        assertThat(launch).isNotEqualTo("the launch values");
        assertThat(launch.hashCode()).isNotEqualTo(changed.hashCode());
    }

    @Test
    void aValueIsOnlyReadInItsOwnUnit() {
        assertThatThrownBy(() -> launch.percentageOf(CHECK_IN_PERIOD)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> launch.durationOf(SERVICE_FEE)).isInstanceOf(IllegalArgumentException.class);
    }
}
