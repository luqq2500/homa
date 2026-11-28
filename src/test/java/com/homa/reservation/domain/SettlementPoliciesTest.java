package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.reservation.domain.ReservationFixtures.friday;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import com.homa.reservation.domain.ReservationValues.CustomerCompensation;
import com.homa.reservation.domain.ReservationValues.ProviderFines;
import com.homa.reservation.domain.ReservationValues.Version;
import com.homa.reservation.domain.pricing.Quote;
import com.homa.reservation.domain.settlement.CustomerLastMinutePolicy;
import com.homa.reservation.domain.settlement.FreeCancellationPolicy;
import com.homa.reservation.domain.settlement.LateCustomerCancellationPolicy;
import com.homa.reservation.domain.settlement.NoRosterPolicy;
import com.homa.reservation.domain.settlement.ProviderCancellationPolicy;
import com.homa.reservation.domain.settlement.ProviderLastMinutePolicy;
import com.homa.reservation.domain.settlement.Settlement;
import com.homa.reservation.domain.settlement.SettlementBasis;
import com.homa.reservation.domain.settlement.SmallerRosterPolicy;
import com.homa.reservation.domain.terms.AcceptancePolicy;
import com.homa.reservation.domain.terms.CancellationTier;
import com.homa.reservation.domain.terms.DiscountRule;
import com.homa.reservation.domain.terms.ReservationTerms;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Each settlement policy reads only what the reservation was booked with: its price and the Homa values in force when
 * it was Requested (P3, RES-8). The standard reservation's price is RM200 with a Base Cancellation Fee of RM45 and
 * RM20 travel. {@code OTHER} are not Homa's values today, so a policy that read the current ones, or another policy's
 * percentage, would give other lines.
 */
class SettlementPoliciesTest {

    private static final Version TODAY = ReservationValues.current();

    /** Values as another time might have set them: every fine, compensation, window and step different, cap 40%. */
    private static final Version OTHER = new Version(TODAY.effectiveFrom(), TODAY.checkInPeriod(),
            TODAY.protectionAfterJobEnd(), TODAY.acceptanceResponseTime(), TODAY.acceptanceCutOffBeforeCheckIn(),
            TODAY.gracePeriod(), TODAY.graceNeedsReservedBeforeCheckIn(), TODAY.freeCancellationBeforeCheckIn(),
            new ProviderFines(Duration.ofHours(36), Percentage.of(15), Percentage.of(17), Percentage.of(25),
                    Percentage.of(35)),
            new CustomerCompensation(Percentage.of(5), Percentage.of(6), Percentage.of(9), Percentage.of(12)),
            TODAY.customerNoShowShareOfLaterHours(), Percentage.of(40), Duration.ofMinutes(30),
            TODAY.completionRemindersAfterJobEnd());

    private static final Quote STANDARD_QUOTE = new Quote(Money.myr(180), Money.myr(20), Money.myr(200),
            Money.myr(45), Percentage.of(10));
    private static final ReservationTerms KEMAS_TERMS = new ReservationTerms(Money.myr(25), Money.myr(20),
            new DiscountRule(Percentage.of(10), Duration.ofHours(3), 2), AcceptancePolicy.REQUEST,
            CancellationTier.STANDARD);
    private static final Quote CHEAP_QUOTE = new Quote(Money.myr(25), Money.myr(0), Money.myr(25), Money.myr(25),
            Percentage.ZERO);
    private static final JobTime JOB = JobTime.of(JOB_START, Duration.ofHours(4));

    private static SettlementBasis basis(Version values) {
        return new SettlementBasis(STANDARD_QUOTE, JOB, values);
    }

    private static Settlement settlement(String fee, String refund, String compensation, String fine) {
        return new Settlement(Money.myr(fee), Money.myr(refund), Money.myr(compensation), Money.myr(fine),
                Money.myr(fee));
    }

    @Test
    @Tag("CAN-3")
    @DisplayName("Percentages in this section are of the Total Reservation Fee, unless stated otherwise. Base + 10% is 45 + 20 = RM65.")
    void aBasisAnswersWhatWasHeldTheCapAndTheCheckInPeriod() {
        SettlementBasis standard = basis(TODAY);

        assertThat(standard.held()).isEqualTo(Money.myr(200));
        assertThat(standard.cap()).isEqualTo(Money.myr(200));
        assertThat(standard.zero()).isEqualTo(Money.myr(0));
        assertThat(standard.checkInStart()).isEqualTo(saturday(9, 45));
        assertThat(basis(OTHER).cap()).isEqualTo(Money.myr(80));
        assertThat(standard.settlement(Money.myr(45), Money.myr(6), Money.myr(65)))
                .isEqualTo(settlement("45", "155", "6", "65"));
    }

    @Test
    @Tag("REQ-2")
    @DisplayName("Aisyah withdraws on Monday 21:00 and gets RM200 back.")
    void aFreeOutcomeRefundsTheWholePayment() {
        assertThat(new FreeCancellationPolicy().settle(basis(TODAY), saturday(9, 0)))
                .isEqualTo(settlement("0", "200", "0", "0"));
        assertThat(new FreeCancellationPolicy().settle(basis(OTHER), saturday(9, 0)))
                .isEqualTo(settlement("0", "200", "0", "0"));
    }

    @Test
    @Tag("CAN-1")
    @DisplayName("The Base Cancellation Fee is RM45, not RM50.")
    void aLateCancellationCostsTheBaseCancellationFeeAfterDiscount() {
        assertThat(new LateCustomerCancellationPolicy().settle(basis(TODAY), friday(22, 0)))
                .isEqualTo(settlement("45", "155", "0", "0"));
        assertThat(new LateCustomerCancellationPolicy().settle(basis(OTHER), friday(22, 0)))
                .isEqualTo(settlement("45", "155", "0", "0"));
    }

    @Test
    @Tag("PCX-2")
    @DisplayName("Kemas Home cancels on Friday 20:00: fined RM65, Aisyah gets RM206.")
    void providerCancellationReadsItsOwnWindowFineAndCompensation() {
        ProviderCancellationPolicy policy = new ProviderCancellationPolicy();
        Instant thirtySixHoursBefore = saturday(9, 45).minus(Duration.ofHours(36));

        assertThat(policy.settle(basis(TODAY), friday(20, 0))).isEqualTo(settlement("0", "200", "6", "65"));
        assertThat(policy.settle(basis(OTHER), friday(20, 0))).isEqualTo(settlement("0", "200", "10", "75"));
        assertThat(policy.settle(basis(OTHER), thirtySixHoursBefore))
                .isEqualTo(settlement("0", "200", "10", "75"));
        assertThat(policy.settle(basis(OTHER), thirtySixHoursBefore.minusSeconds(60)))
                .isEqualTo(settlement("0", "200", "0", "45"));
    }

    @Test
    @Tag("LMC-3")
    @DisplayName("Kemas Home's van breaks down at 9:50: fined RM85, Aisyah gets RM214.")
    void providerLastMinuteReadsItsOwnFineAndCompensation() {
        ProviderLastMinutePolicy policy = new ProviderLastMinutePolicy();

        assertThat(policy.settle(basis(TODAY), saturday(9, 50))).isEqualTo(settlement("0", "200", "14", "85"));
        assertThat(policy.settle(basis(OTHER), saturday(9, 50))).isEqualTo(settlement("0", "200", "18", "80"));
    }

    @Test
    @Tag("NRS-2")
    @Tag("NRS-3")
    @DisplayName("At 9:45 only Siti is rostered for 2 helpers: Kemas Home is fined RM69 and Aisyah gets RM208.")
    void noRosterReadsItsOwnFineAndCompensation() {
        NoRosterPolicy policy = new NoRosterPolicy();

        assertThat(policy.settle(basis(TODAY), saturday(9, 45))).isEqualTo(settlement("0", "200", "8", "69"));
        assertThat(policy.settle(basis(OTHER), saturday(9, 45))).isEqualTo(settlement("0", "200", "12", "79"));
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Kemas Home offers Siti alone: 25 × 4 × 1 × 0.9 + 20 = RM110, so Aisyah gets RM90 back, with no fine and no compensation.")
    void aSmallerRosterIsRequotedKeepingTheBookingDiscountAndTheDifferenceIsRefunded() {
        SmallerRosterPolicy sitiAlone = new SmallerRosterPolicy(KEMAS_TERMS, 1);

        assertThat(sitiAlone.requote(basis(TODAY))).isEqualTo(
                new Quote(Money.myr(90), Money.myr(20), Money.myr(110), Money.myr("22.5"), Percentage.of(10)));
        assertThat(sitiAlone.settle(basis(TODAY), friday(10, 0))).isEqualTo(settlement("110", "90", "0", "0"));
        assertThat(new SmallerRosterPolicy(KEMAS_TERMS, 2).settle(basis(TODAY), friday(10, 0)))
                .as("the same number of helpers costs the same").isEqualTo(settlement("200", "0", "0", "0"));
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("The price now is what the smaller roster is worked out from: RM200 for 2 helpers, then Siti alone is RM110 and RM90 back; a booking with no discount keeps none.")
    void aSmallerRosterIsWorkedOutFromThePriceNowAndTheDiscountItHad() {
        Quote noDiscount = new Quote(Money.myr(100), Money.myr(20), Money.myr(120), Money.myr(50), Percentage.ZERO);
        JobTime twoHours = JobTime.of(JOB_START, Duration.ofHours(2));
        SettlementBasis twoHelpersTwoHours = new SettlementBasis(noDiscount, twoHours, TODAY);

        assertThat(new SmallerRosterPolicy(KEMAS_TERMS, 1).settle(twoHelpersTwoHours, friday(10, 0)))
                .isEqualTo(settlement("70", "50", "0", "0"));
        assertThat(new SmallerRosterPolicy(KEMAS_TERMS, 1).requote(twoHelpersTwoHours).discountRate())
                .isEqualTo(Percentage.ZERO);
    }

    @Test
    @Tag("CAN-2")
    @DisplayName("The new price of a smaller roster is the job's price, not a fee: a cap on fees and fines below it doesn't change what Aisyah gets back.")
    void theCapOnFeesDoesNotLimitTheNewPriceOfASmallerRoster() {
        assertThat(new SmallerRosterPolicy(KEMAS_TERMS, 1).settle(basis(OTHER), friday(10, 0)))
                .isEqualTo(settlement("110", "90", "0", "0"));
    }

    @Test
    @Tag("LMC-2")
    @DisplayName("Aisyah stops the job at 11:20: 30 minutes are charged (RM22.50), so she pays RM87.50 and gets RM112.50 back.")
    void customerLastMinuteReadsItsOwnPartialTimeStep() {
        CustomerLastMinutePolicy policy = new CustomerLastMinutePolicy();

        assertThat(policy.settle(basis(TODAY), saturday(11, 20))).isEqualTo(settlement("87.50", "112.50", "0", "0"));
        assertThat(policy.settle(basis(OTHER), saturday(11, 20))).isEqualTo(settlement("80", "120", "0", "0"));
        assertThat(policy.settle(basis(TODAY), saturday(11, 5))).isEqualTo(settlement("76.25", "123.75", "0", "0"));
        assertThat(policy.settle(basis(OTHER), saturday(11, 5))).isEqualTo(settlement("80", "120", "0", "0"));
    }

    @Test
    @Tag("CAN-2")
    @DisplayName("No fee or fine is more than the Total Reservation Fee: under a cap of 40% the policies stop at RM80.")
    void everyPolicyAppliesTheCapOfTheValuesItReads() {
        SettlementBasis cappedAtForty = basis(OTHER);

        assertThat(new ProviderLastMinutePolicy().settle(cappedAtForty, saturday(9, 50)).providerFine())
                .isEqualTo(Money.myr(80));
        assertThat(new CustomerLastMinutePolicy().settle(cappedAtForty, saturday(13, 59)))
                .isEqualTo(settlement("80", "120", "0", "0"));
        assertThat(new LateCustomerCancellationPolicy().settle(cappedAtForty, friday(22, 0)).customerFee())
                .isEqualTo(Money.myr(45));
        assertThat(new NoRosterPolicy().settle(new SettlementBasis(CHEAP_QUOTE, JOB, TODAY), saturday(9, 45)))
                .as("a RM25 job: 25 + 12% of 25 is RM28, capped at RM25")
                .isEqualTo(new Settlement(Money.myr(0), Money.myr(25), Money.myr(1), Money.myr(25), Money.myr(0)));
    }
}
