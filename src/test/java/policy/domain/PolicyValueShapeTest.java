package policy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kernel.Percentage;
import policy.domain.PolicyValue.BookingHorizon;
import policy.domain.PolicyValue.CancellationTierFee;
import policy.domain.PolicyValue.CheckInPeriod;
import policy.domain.PolicyValue.CheckInRadius;
import policy.domain.PolicyValue.CompletionDeadline;
import policy.domain.PolicyValue.CompletionReminder;
import policy.domain.PolicyValue.CustomerNoShowFee;
import policy.domain.PolicyValue.CustomerNoShowWait;
import policy.domain.PolicyValue.DisputeWindowExtension;
import policy.domain.PolicyValue.DurationRange;
import policy.domain.PolicyValue.EarliestCheckIn;
import policy.domain.PolicyValue.EvidenceRetention;
import policy.domain.PolicyValue.FeeWindow;
import policy.domain.PolicyValue.FineDeductionCap;
import policy.domain.PolicyValue.HelpersPerSearch;
import policy.domain.PolicyValue.MinimumNotice;
import policy.domain.PolicyValue.MinimumRatingsToShow;
import policy.domain.PolicyValue.NoShowReportWindow;
import policy.domain.PolicyValue.ProviderLateCancellationFine;
import policy.domain.PolicyValue.ProviderNoShowFine;
import policy.domain.PolicyValue.RequestExpiry;
import policy.domain.PolicyValue.RatingWindow;
import policy.domain.PolicyValue.ServiceFee;
import policy.domain.PolicyValue.TeamReminder;
import policy.domain.exception.CheckInRadiusNotPositiveException;
import policy.domain.exception.InvalidPolicyRangeException;
import policy.domain.exception.MinimumRatingsToShowNotPositiveException;
import policy.domain.exception.PolicyPeriodNotPositiveException;
import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/** The shape each Homa-set value must have, before any rule across values. */
class PolicyValueShapeTest {

    @Nested
    class Periods {

        static Stream<Named<Function<Duration, PolicyValue>>> periods() {
            return Stream.of(
                    Named.of("minimum notice", MinimumNotice::new),
                    Named.of("booking horizon", BookingHorizon::new),
                    Named.of("request expiry", RequestExpiry::new),
                    Named.of("fee window", FeeWindow::new),
                    Named.of("team reminder", TeamReminder::new),
                    Named.of("earliest check-in", EarliestCheckIn::new),
                    Named.of("check-in period", CheckInPeriod::new),
                    Named.of("customer no-show wait", CustomerNoShowWait::new),
                    Named.of("no-show report window", NoShowReportWindow::new),
                    Named.of("completion reminder", CompletionReminder::new),
                    Named.of("completion deadline", CompletionDeadline::new),
                    Named.of("dispute window extension", DisputeWindowExtension::new),
                    Named.of("evidence retention", EvidenceRetention::new),
                    Named.of("rating window", RatingWindow::new));
        }

        @ParameterizedTest(name = "the {0} must be longer than zero")
        @MethodSource("periods")
        void everyHomaSetPeriodIsLongerThanZero(Function<Duration, PolicyValue> period) {
            assertThatThrownBy(() -> period.apply(Duration.ZERO)).isInstanceOf(PolicyPeriodNotPositiveException.class);
            assertThatThrownBy(() -> period.apply(Duration.ofMinutes(-15)))
                    .isInstanceOf(PolicyPeriodNotPositiveException.class);
        }

        @ParameterizedTest(name = "the {0} can be as short as a second")
        @MethodSource("periods")
        void anyPeriodLongerThanZeroIsAccepted(Function<Duration, PolicyValue> period) {
            assertThatCode(() -> period.apply(Duration.ofSeconds(1))).doesNotThrowAnyException();
        }

        @Test
        void aRejectedPeriodIsNamedInTheMessage() {
            assertThatThrownBy(() -> new CheckInPeriod(Duration.ZERO))
                    .hasMessage("The check-in period must be longer than zero, was PT0S");
        }
    }

    @Nested
    class Ranges {

        static Stream<Named<BiFunction<Integer, Integer, PolicyValue>>> ranges() {
            return Stream.of(
                    Named.of("duration range", DurationRange::new),
                    Named.of("helpers per search", HelpersPerSearch::new));
        }

        @ParameterizedTest(name = "the {0} must start at 1 or more")
        @MethodSource("ranges")
        void aRangeStartsAtOneOrMore(BiFunction<Integer, Integer, PolicyValue> range) {
            assertThatThrownBy(() -> range.apply(0, 6)).isInstanceOf(InvalidPolicyRangeException.class);
            assertThatCode(() -> range.apply(1, 6)).doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "the {0} minimum cannot be above its maximum")
        @MethodSource("ranges")
        void aRangeMinimumIsNotAboveItsMaximum(BiFunction<Integer, Integer, PolicyValue> range) {
            assertThatThrownBy(() -> range.apply(4, 3)).isInstanceOf(InvalidPolicyRangeException.class);
            assertThatCode(() -> range.apply(3, 3)).doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "{0} to {1} hours")
        @CsvSource({"1, 6", "2, 4"})
        void aRangeKeepsItsBounds(int min, int max) {
            assertThat(new DurationRange(min, max).minHours()).isEqualTo(min);
            assertThat(new DurationRange(min, max).maxHours()).isEqualTo(max);
            assertThat(new HelpersPerSearch(min, max).min()).isEqualTo(min);
            assertThat(new HelpersPerSearch(min, max).max()).isEqualTo(max);
        }

        @Test
        void aRejectedRangeIsShownInTheMessage() {
            assertThatThrownBy(() -> new DurationRange(4, 3)).hasMessageContaining("4 to 3");
        }
    }

    @Test
    void averagesAreShownAfterAtLeastOneRating() {
        assertThatThrownBy(() -> new MinimumRatingsToShow(0)).isInstanceOf(MinimumRatingsToShowNotPositiveException.class);
        assertThat(new MinimumRatingsToShow(1).ratings()).isEqualTo(1);
    }

    @Test
    void theCheckInRadiusIsMoreThanZeroMetres() {
        assertThatThrownBy(() -> new CheckInRadius(0)).isInstanceOf(CheckInRadiusNotPositiveException.class);
        assertThat(new CheckInRadius(1).metres()).isEqualTo(1);
    }

    static Stream<Named<Function<Percentage, PolicyValue>>> shares() {
        return Stream.of(
                Named.of("service fee", ServiceFee::new),
                Named.of("customer no-show fee", CustomerNoShowFee::new),
                Named.of("late cancellation fine", ProviderLateCancellationFine::new),
                Named.of("no-show fine", ProviderNoShowFine::new),
                Named.of("fine deduction cap", FineDeductionCap::new),
                Named.of("Standard tier fee", share -> new CancellationTierFee(CancellationTier.STANDARD, share)));
    }

    @ParameterizedTest(name = "the {0} needs a share")
    @MethodSource("shares")
    void everyShareNeedsAValue(Function<Percentage, PolicyValue> share) {
        assertThatThrownBy(() -> share.apply(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void aTierFeeNeedsATier() {
        assertThatThrownBy(() -> new CancellationTierFee(null, Percentage.of(10))).isInstanceOf(NullPointerException.class);
    }

    @Test
    void aTierFeeIsKeyedByItsTierAndEveryOtherValueByItsKind() {
        assertThat(new CancellationTierFee(CancellationTier.FIRM, Percentage.of(20)).key()).isEqualTo(CancellationTier.FIRM);
        assertThat(new ServiceFee(Percentage.of(15)).key()).isEqualTo(ServiceFee.class);
    }
}
