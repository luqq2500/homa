package policy.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static policy.usecase.PolicyUsecaseFixtures.A_WEEK_LATER;
import static policy.usecase.PolicyUsecaseFixtures.FOUNDER;
import static policy.usecase.PolicyUsecaseFixtures.LAUNCH;
import static policy.usecase.PolicyUsecaseFixtures.at;

import ie.events.HomaPolicyChangedIE;
import java.time.Duration;
import java.util.stream.Stream;
import kernel.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import policy.domain.CancellationTier;
import policy.domain.PolicySnapshot;
import policy.domain.exception.CheckInRadiusNotPositiveException;
import policy.domain.exception.FlexibleTierMustBeFreeException;
import policy.domain.exception.InvalidPolicyRangeException;
import policy.domain.exception.PolicyPeriodNotPositiveException;
import policy.usecase.PolicyUsecaseFixtures.AuditLog;
import policy.usecase.PolicyUsecaseFixtures.InMemoryPolicies;
import policy.usecase.PolicyUsecaseFixtures.PublishedEvents;
import policy.usecase.dto.ChangePolicyValueRequest;
import policy.usecase.dto.ChangePolicyValueResponse;
import policy.usecase.dto.HomaViewPolicyResponse;
import policy.usecase.dto.LaunchPolicyRequest;
import policy.usecase.PolicyUsecaseFixtures.AnotherFounderChangesFirst;
import policy.usecase.exception.AdminReasonRequiredException;
import policy.usecase.exception.ChangedMeanwhileException;
import policy.usecase.exception.DecidedByRequiredException;
import policy.usecase.exception.InvalidPolicyAmountException;
import policy.usecase.exception.PolicyNotLaunchedException;
import policy.usecase.exception.UnknownPolicyValueException;
import policy.usecase.port.out.AdminAction;

/** Homa's founders change one Homa-set value at a time; each change is a new version for new reservations. */
class HomaPolicyChangeTest {

    private static final String WHY = "Matching the market after launch";

    private final InMemoryPolicies policies = new InMemoryPolicies();
    private final PublishedEvents events = new PublishedEvents();
    private final AuditLog auditLog = new AuditLog();
    private final HomaChangePolicyValue change = new HomaChangePolicyValue(policies, events, auditLog, at(A_WEEK_LATER));
    private final HomaViewPolicy view = new HomaViewPolicy(policies);

    private ChangePolicyValueResponse changing(String value, String amount) {
        return change.change(new ChangePolicyValueRequest(FOUNDER, value, amount, WHY));
    }

    private PolicySnapshot inForce() {
        return policies.findCurrent().orElseThrow().snapshot();
    }

    private String shown(String value) {
        return view.view().values().stream().filter(v -> v.name().equals(value)).findFirst().orElseThrow().amount();
    }

    private void nothingChanged() {
        assertThat(policies.versions).hasSize(1);
        assertThat(events.all).hasSize(1);
        assertThat(auditLog.all).hasSize(1);
    }

    @Nested
    class AfterLaunch {

        @BeforeEach
        void theLaunchValuesAreInForce() {
            new HomaLaunchPolicy(policies, events, auditLog, at(LAUNCH)).launch(new LaunchPolicyRequest(FOUNDER));
        }

        @Test
        void aChangeIsSavedAsTheNextVersionInForceFromNow() {
            ChangePolicyValueResponse changed = changing("SERVICE_FEE", "17.5%");

            assertThat(changed).isEqualTo(new ChangePolicyValueResponse(2, A_WEEK_LATER, "SERVICE_FEE", "17.5%"));
            assertThat(changed.version()).isEqualTo(2);
            assertThat(changed.effectiveFrom()).isEqualTo(A_WEEK_LATER);
            assertThat(changed.value()).isEqualTo("SERVICE_FEE");
            assertThat(changed.amount()).isEqualTo("17.5%");
            assertThat(policies.versions).hasSize(2);
            assertThat(inForce().serviceFee()).isEqualTo(Percentage.ofBasisPoints(1750));
        }

        @Test
        void theVersionBeforeIsKeptForReservationsAlreadyRequested() {
            changing("SERVICE_FEE", "17.5%");

            assertThat(policies.versions.get(0).snapshot().serviceFee()).isEqualTo(Percentage.of(15));
        }

        @Test
        void theOtherContextsAreToldTheWholeNewVersion() {
            changing("CHECK_IN_RADIUS", "250");

            assertThat(events.all).hasSize(2);
            HomaPolicyChangedIE told = (HomaPolicyChangedIE) events.all.get(1);
            assertThat(told.version()).isEqualTo(2);
            assertThat(told.effectiveFrom()).isEqualTo(A_WEEK_LATER);
            assertThat(told.occurredAt()).isEqualTo(A_WEEK_LATER);
            assertThat(told.checkInRadiusMetres()).isEqualTo(250);
            assertThat(told.serviceFee()).isEqualTo(Percentage.of(15));
        }

        @Test
        void aChangeIsRecordedWithTheNewAmountAndItsReason() {
            change.change(new ChangePolicyValueRequest(FOUNDER, "SERVICE_FEE", "17.5", "  " + WHY + " "));

            assertThat(auditLog.all).last()
                    .isEqualTo(new AdminAction(FOUNDER, "CHANGE_POLICY_VALUE", "SERVICE_FEE=17.5%", WHY, A_WEEK_LATER));
        }

        @ParameterizedTest(name = "reason \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "  "})
        void aChangeNeedsAReason(String reason) {
            assertThatThrownBy(() -> change.change(new ChangePolicyValueRequest(FOUNDER, "SERVICE_FEE", "17.5%", reason)))
                    .isInstanceOf(AdminReasonRequiredException.class)
                    .hasMessage("A reason is required to change a Homa-set value");
            nothingChanged();
        }

        @Test
        void aChangeNeedsTheFounderWhoDecidedIt() {
            assertThatThrownBy(() -> change.change(new ChangePolicyValueRequest(null, "SERVICE_FEE", "17.5%", WHY)))
                    .isInstanceOf(DecidedByRequiredException.class);
            nothingChanged();
        }

        @Test
        void aValueThatDoesNotExistIsRefused() {
            assertThatThrownBy(() -> changing("TIP_JAR", "5%"))
                    .isInstanceOf(UnknownPolicyValueException.class)
                    .hasMessage("There is no Homa-set value named TIP_JAR");
            nothingChanged();
        }

        @Test
        void aValueIsNamedExactly() {
            assertThatThrownBy(() -> changing("service_fee", "5%")).isInstanceOf(UnknownPolicyValueException.class);
        }

        @Test
        void aChangeThatBreaksARuleAcrossValuesIsRefusedByTheDomain() {
            assertThatThrownBy(() -> changing("FLEXIBLE_CANCELLATION_FEE", "5%")).isInstanceOf(FlexibleTierMustBeFreeException.class);
            nothingChanged();
        }

        @Test
        void aValueOfTheWrongShapeIsRefusedByTheDomain() {
            assertThatThrownBy(() -> changing("CHECK_IN_RADIUS", "0")).isInstanceOf(CheckInRadiusNotPositiveException.class);
            assertThatThrownBy(() -> changing("TEAM_REMINDER", "PT0S")).isInstanceOf(PolicyPeriodNotPositiveException.class);
            assertThatThrownBy(() -> changing("DURATION_RANGE", "6-1")).isInstanceOf(InvalidPolicyRangeException.class);
            nothingChanged();
        }

        static Stream<Arguments> everyValue() {
            return Stream.of(
                    Arguments.of("SERVICE_FEE", "12.5%", "12.5%"),
                    Arguments.of("DURATION_RANGE", "2-8", "2-8"),
                    Arguments.of("MINIMUM_NOTICE", "PT3H", "PT3H"),
                    Arguments.of("BOOKING_HORIZON", "P60D", "P60D"),
                    Arguments.of("REQUEST_EXPIRY", "PT12H", "PT12H"),
                    Arguments.of("RESPONSE_DEADLINE", "PT30M", "PT30M"),
                    Arguments.of("FEE_WINDOW", "PT48H", "P2D"),
                    Arguments.of("FLEXIBLE_CANCELLATION_FEE", "0%", "0%"),
                    Arguments.of("STANDARD_CANCELLATION_FEE", "15%", "15%"),
                    Arguments.of("FIRM_CANCELLATION_FEE", "25%", "25%"),
                    Arguments.of("PROVIDER_LATE_CANCELLATION_FINE", "25%", "25%"),
                    Arguments.of("PROVIDER_NO_SHOW_FINE", "35%", "35%"),
                    Arguments.of("FINE_DEDUCTION_CAP", "40%", "40%"),
                    Arguments.of("TEAM_REMINDER", "PT8H", "PT8H"),
                    Arguments.of("EARLIEST_CHECK_IN", "PT20M", "PT20M"),
                    Arguments.of("CHECK_IN_PERIOD", "PT25M", "PT25M"),
                    Arguments.of("CHECK_IN_RADIUS", "250", "250"),
                    Arguments.of("CUSTOMER_NO_SHOW_WAIT", "PT10M", "PT10M"),
                    Arguments.of("CUSTOMER_NO_SHOW_FEE", "35%", "35%"),
                    Arguments.of("NO_SHOW_REPORT_WINDOW", "PT2H", "PT2H"),
                    Arguments.of("COMPLETION_REMINDER", "PT45M", "PT45M"),
                    Arguments.of("COMPLETION_DEADLINE", "PT36H", "PT36H"),
                    Arguments.of("DISPUTE_WINDOW_EXTENSION", "PT90M", "PT1H30M"),
                    Arguments.of("EVIDENCE_RETENTION", "P10D", "P10D"),
                    Arguments.of("RATING_WINDOW", "P21D", "P21D"),
                    Arguments.of("MINIMUM_RATINGS_TO_SHOW", "5", "5"));
        }

        @ParameterizedTest(name = "{0} changed to {1} is shown as {2}")
        @MethodSource("everyValue")
        void everyHomaSetValueCanBeChangedAndIsShownAsChanged(String value, String amount, String shownAs) {
            String before = shown(value);

            ChangePolicyValueResponse changed = changing(value, amount);

            assertThat(changed.amount()).isEqualTo(shownAs);
            assertThat(shown(value)).isEqualTo(shownAs);
            assertThat(view.view().version()).isEqualTo(2);
            if (!value.equals("FLEXIBLE_CANCELLATION_FEE")) {
                assertThat(before).isNotEqualTo(shownAs);
            }
        }

        @Test
        void changingOneValueLeavesEveryOtherValueAsItWas() {
            HomaViewPolicyResponse before = view.view();

            changing("FIRM_CANCELLATION_FEE", "25%");

            HomaViewPolicyResponse after = view.view();
            for (int i = 0; i < before.values().size(); i++) {
                if (!before.values().get(i).name().equals("FIRM_CANCELLATION_FEE")) {
                    assertThat(after.values().get(i)).isEqualTo(before.values().get(i));
                }
            }
            assertThat(inForce().cancellationFee(CancellationTier.FIRM)).isEqualTo(Percentage.of(25));
            assertThat(inForce().cancellationFee(CancellationTier.STANDARD)).isEqualTo(Percentage.of(10));
        }

        @Test
        void aPeriodOfWholeDaysIsShownInDaysAndAnyOtherInHoursAndMinutes() {
            changing("FEE_WINDOW", "P1D");
            assertThat(shown("FEE_WINDOW")).isEqualTo("P1D");
            changing("FEE_WINDOW", "PT25H");
            assertThat(shown("FEE_WINDOW")).isEqualTo("PT25H");
            changing("FEE_WINDOW", "PT24H1M");
            assertThat(shown("FEE_WINDOW")).isEqualTo("PT24H1M");
            assertThat(inForce().feeWindow()).isEqualTo(Duration.ofHours(24).plusMinutes(1));
        }

        @Test
        void aChangeIsRefusedWhenAnotherFounderChangedAValueFirstAndTheirChangeIsKept() {
            AnotherFounderChangesFirst racing = new AnotherFounderChangesFirst();
            HomaLaunchPolicy launchIt = new HomaLaunchPolicy(racing, events, auditLog, at(LAUNCH));
            HomaChangePolicyValue changeIt = new HomaChangePolicyValue(racing, events, auditLog, at(A_WEEK_LATER));
            launchIt.launch(new LaunchPolicyRequest(FOUNDER));
            changeIt.change(new ChangePolicyValueRequest(FOUNDER, "SERVICE_FEE", "17%", WHY));
            int toldBefore = events.all.size();
            int recordedBefore = auditLog.all.size();

            assertThatThrownBy(() -> changeIt.change(new ChangePolicyValueRequest("founder-ben", "CHECK_IN_RADIUS", "250", WHY)))
                    .isInstanceOf(ChangedMeanwhileException.class)
                    .hasMessage("The Homa-set values changed meanwhile; try again on what is there now");

            assertThat(racing.versions).hasSize(2);
            assertThat(racing.versions.get(1).snapshot().serviceFee()).isEqualTo(Percentage.of(17));
            assertThat(events.all).hasSize(toldBefore);
            assertThat(auditLog.all).hasSize(recordedBefore);
        }
    }

    @Nested
    class BeforeLaunch {

        @Test
        void noValueCanBeChangedBeforeThePolicyIsLaunched() {
            assertThatThrownBy(() -> changing("SERVICE_FEE", "17.5%"))
                    .isInstanceOf(PolicyNotLaunchedException.class)
                    .hasMessage("The policy has not been launched yet");
            assertThat(policies.versions).isEmpty();
            assertThat(events.all).isEmpty();
            assertThat(auditLog.all).isEmpty();
        }

        @Test
        void theValuesCannotBeViewedBeforeThePolicyIsLaunched() {
            assertThatThrownBy(view::view).isInstanceOf(PolicyNotLaunchedException.class);
        }
    }

    @Nested
    class AmountsAsTheConsoleWritesThem {

        @BeforeEach
        void theLaunchValuesAreInForce() {
            new HomaLaunchPolicy(policies, events, auditLog, at(LAUNCH)).launch(new LaunchPolicyRequest(FOUNDER));
        }

        @ParameterizedTest(name = "\"{0}\" is {1} basis points")
        @MethodSource("percentages")
        void aPercentageIsWrittenWithUpToTwoDecimalsWithOrWithoutThePercentSign(String amount, int basisPoints) {
            changing("CUSTOMER_NO_SHOW_FEE", amount);

            assertThat(inForce().customerNoShowFee()).isEqualTo(Percentage.ofBasisPoints(basisPoints));
        }

        static Stream<Arguments> percentages() {
            return Stream.of(
                    Arguments.of("35", 3500), Arguments.of("35%", 3500), Arguments.of(" 35% ", 3500),
                    Arguments.of("35.5", 3550), Arguments.of("35.25%", 3525), Arguments.of("100%", 10000),
                    Arguments.of("100.00", 10000));
        }

        @Test
        void aPercentageBelowOneIsWrittenWithASingleLeadingZero() {
            changing("SERVICE_FEE", "0.5%");
            assertThat(inForce().serviceFee()).isEqualTo(Percentage.ofBasisPoints(50));
            changing("SERVICE_FEE", "0");
            assertThat(inForce().serviceFee()).isEqualTo(Percentage.ZERO);
        }

        @ParameterizedTest(name = "\"{0}\" is not a percentage")
        @NullSource
        @ValueSource(strings = {"", "abc", "35.255%", "100.01%", "101", "1000%", "-5%", "35 %", "%", "030", "00.5"})
        void anAmountThatIsNotAPercentageFromNoneToAllIsRefused(String amount) {
            assertThatThrownBy(() -> changing("CUSTOMER_NO_SHOW_FEE", amount))
                    .isInstanceOf(InvalidPolicyAmountException.class)
                    .hasMessageStartingWith("CUSTOMER_NO_SHOW_FEE needs a percentage");
            nothingChanged();
        }

        @Test
        void aPercentageAboveAllIsRefusedWithWhatIsAllowed() {
            assertThatThrownBy(() -> changing("SERVICE_FEE", "100.5"))
                    .hasMessage("SERVICE_FEE needs a percentage from 0% to 100%, was 100.5");
        }

        @Test
        void aPercentageThatIsNotWrittenAsOneSaysHowToWriteIt() {
            assertThatThrownBy(() -> changing("SERVICE_FEE", "fifteen"))
                    .hasMessage("SERVICE_FEE needs a percentage such as 17.5%, was fifteen");
        }

        @ParameterizedTest(name = "\"{0}\" is not a period")
        @NullSource
        @ValueSource(strings = {"", "2 hours", "2h", "PT", "PT2H30S", "PT2H0.5S", "PT90S"})
        void anAmountThatIsNotAPeriodOfWholeMinutesIsRefused(String amount) {
            assertThatThrownBy(() -> changing("MINIMUM_NOTICE", amount))
                    .isInstanceOf(InvalidPolicyAmountException.class)
                    .hasMessageStartingWith("MINIMUM_NOTICE needs a period of whole minutes such as PT2H or P30D, was ");
            nothingChanged();
        }

        @Test
        void aPeriodIsReadWithoutSpacesAroundIt() {
            changing("MINIMUM_NOTICE", " PT3H ");

            assertThat(inForce().minimumNotice()).isEqualTo(Duration.ofHours(3));
        }

        @ParameterizedTest(name = "\"{0}\" is not a whole number")
        @NullSource
        @ValueSource(strings = {"", "-1", "2.5", "three", "1234567890", " 3 0 ", "05"})
        void anAmountThatIsNotAWholeNumberIsRefused(String amount) {
            assertThatThrownBy(() -> changing("MINIMUM_RATINGS_TO_SHOW", amount))
                    .isInstanceOf(InvalidPolicyAmountException.class)
                    .hasMessageStartingWith("MINIMUM_RATINGS_TO_SHOW needs a whole number such as 300, was ");
            nothingChanged();
        }

        @Test
        void aWholeNumberIsReadWithoutSpacesAroundItUpToNineDigits() {
            changing("CHECK_IN_RADIUS", " 123456789 ");

            assertThat(inForce().checkInRadiusMetres()).isEqualTo(123456789);
        }

        @ParameterizedTest(name = "\"{0}\" is not a range")
        @NullSource
        @ValueSource(strings = {"", "1 - 6", "1to6", "-6", "1-", "12345-6", "1-12345", "0-6", "01-6", "1-06"})
        void anAmountThatIsNotARangeOfHoursIsRefused(String amount) {
            assertThatThrownBy(() -> changing("DURATION_RANGE", amount))
                    .isInstanceOf(InvalidPolicyAmountException.class)
                    .hasMessageStartingWith("DURATION_RANGE needs a range of hours such as 1-6, was ");
            nothingChanged();
        }

        @Test
        void aRangeIsReadWithoutSpacesAroundItUpToFourDigitsEachSide() {
            changing("DURATION_RANGE", " 1-1000 ");

            assertThat(inForce().durationRange().minHours()).isEqualTo(1);
            assertThat(inForce().durationRange().maxHours()).isEqualTo(1000);
        }
    }
}
