package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.fixtures.HomaFixtures.STANDARD_HELPERS;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.reservation.domain.ReservationFixtures.ACCEPTED_AT;
import static com.homa.reservation.domain.ReservationFixtures.aisyahBooking;
import static com.homa.reservation.domain.ReservationFixtures.friday;
import static com.homa.reservation.domain.ReservationFixtures.kemasHomeProvider;
import static com.homa.reservation.domain.ReservationFixtures.meiBooking;
import static com.homa.reservation.domain.ReservationFixtures.monday;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.standardBooking;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.ReservationValues.CustomerCompensation;
import com.homa.reservation.domain.ReservationValues.ProviderFines;
import com.homa.reservation.domain.ReservationValues.Version;
import com.homa.reservation.domain.exception.JobAlreadyEndedException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.domain.replica.BookingBounds;
import com.homa.reservation.domain.replica.TravelFeeRule;
import com.homa.reservation.domain.settlement.Settlement;
import com.homa.reservation.domain.terms.CancellationTier;
import com.homa.reservation.domain.terms.DiscountRule;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** The cancellation of a Reserved reservation by its customer or its provider (§6.2, §6.3, §6.5). */
class ReservationCancellationTest {

    private static final Version VALUES = ReservationValues.current();

    private final Marketplace market = Marketplace.withKemasHome();
    private final Reservation reservation = market.reserveStandard();

    private static Instant wednesday(int hour, int minute) {
        return at(2026, 12, 2, hour, minute);
    }

    private static Instant thursday(int hour, int minute) {
        return at(2026, 12, 3, hour, minute);
    }

    private static Settlement settlement(String fee, String refund, String compensation, String fine) {
        return new Settlement(Money.myr(fee), Money.myr(refund), Money.myr(compensation), Money.myr(fine),
                Money.myr(fee));
    }

    /** A reservation Reserved at the given moment by a provider with this tier, for Saturday 10:00, 4 hours. */
    private static Reservation reservedAt(Instant paidAt, CancellationTier tier) {
        Marketplace tiered = new Marketplace(kemasHomeProvider().tier(tier).build());
        Reservation requested = tiered.book(standardBooking(), paidAt.minusSeconds(120), paidAt);
        requested.accept(paidAt);
        return requested;
    }

    // Customer: Grace Period (CCX-1)

    @Test
    @Tag("CCX-1")
    @DisplayName("Reserved Monday 22:00, Aisyah cancels at 23:30 for free.")
    void customerCancelsForFreeWithinTheGracePeriod() {
        Cancellation cancelled = reservation.cancelByCustomer(monday(23, 30));

        assertThat(cancelled).isEqualTo(new Cancellation(monday(23, 30), CancellationCause.CUSTOMER_CANCELLED,
                settlement("0", "200", "0", "0")));
        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.cancellation()).contains(cancelled);
    }

    @Test
    @Tag("CCX-1")
    @DisplayName("Reserved with a Firm provider on Friday 8:00, she can still cancel for free at 11:00.")
    void gracePeriodAppliesWhateverTheTier() {
        Reservation firm = reservedAt(friday(8, 0), CancellationTier.FIRM);

        Cancellation cancelled = firm.cancelByCustomer(friday(11, 0));

        assertThat(cancelled.cause()).isEqualTo(CancellationCause.CUSTOMER_CANCELLED);
        assertThat(cancelled.settlement()).isEqualTo(settlement("0", "200", "0", "0"));
    }

    @Test
    @Tag("CCX-1")
    @DisplayName("The Grace Period is 4 hours: free at 11:59 for a booking Reserved Friday 8:00 with a Firm provider, and from 12:00 the tier's cut-off, long past, decides.")
    void gracePeriodEndsFourHoursAfterReserved() {
        Reservation firmAtLastMinute = reservedAt(friday(8, 0), CancellationTier.FIRM);
        Reservation firmAfterGrace = reservedAt(friday(8, 0), CancellationTier.FIRM);

        assertThat(firmAtLastMinute.cancelByCustomer(friday(11, 59)).settlement().customerFee())
                .isEqualTo(Money.myr(0));
        assertThat(firmAfterGrace.cancelByCustomer(friday(12, 0)).settlement().customerFee())
                .isEqualTo(Money.myr(45));
    }

    @Test
    @Tag("CCX-1")
    @DisplayName("The Grace Period needs the reservation Reserved at least 24 hours before the Check-in Period: Friday 9:45 is, Friday 9:46 is not.")
    void gracePeriodNeedsTwentyFourHoursBeforeTheCheckInPeriod() {
        Reservation reservedExactlyInTime = reservedAt(friday(9, 45), CancellationTier.FIRM);
        Reservation reservedAMinuteLate = reservedAt(friday(9, 46), CancellationTier.FIRM);

        assertThat(reservedExactlyInTime.cancelByCustomer(friday(10, 0)).settlement().customerFee())
                .isEqualTo(Money.myr(0));
        assertThat(reservedAMinuteLate.cancelByCustomer(friday(10, 0)).settlement().customerFee())
                .isEqualTo(Money.myr(45));
    }

    // Customer: tiers (CCX-2 to CCX-6)

    @Test
    @Tag("CCX-2")
    @DisplayName("Aisyah cancels on Wednesday, so Kemas Home's Standard tier applies.")
    void customerCancelsForFreeOnWednesdayUnderStandard() {
        Cancellation cancelled = reservation.cancelByCustomer(wednesday(10, 0));

        assertThat(cancelled.cause()).isEqualTo(CancellationCause.CUSTOMER_CANCELLED);
        assertThat(cancelled.settlement()).isEqualTo(settlement("0", "200", "0", "0"));
    }

    @Test
    @Tag("CCX-3")
    @DisplayName("Flexible: free until Saturday 5:45.")
    void flexibleTierIsFreeUntilFourHoursBeforeTheCheckInPeriod() {
        Reservation beforeCutOff = reservedAt(monday(22, 0), CancellationTier.FLEXIBLE);
        Reservation atCutOff = reservedAt(monday(22, 0), CancellationTier.FLEXIBLE);

        assertThat(beforeCutOff.cancelByCustomer(saturday(5, 44)).settlement()).isEqualTo(
                settlement("0", "200", "0", "0"));
        assertThat(atCutOff.cancelByCustomer(saturday(5, 45)).settlement()).isEqualTo(
                settlement("45", "155", "0", "0"));
    }

    @Test
    @Tag("CCX-4")
    @DisplayName("Standard: free until Friday 21:45.")
    void standardTierIsFreeUntilTwelveHoursBeforeTheCheckInPeriod() {
        Reservation beforeCutOff = reservation;
        Reservation atCutOff = Marketplace.withKemasHome().reserveStandard();

        assertThat(beforeCutOff.cancelByCustomer(friday(21, 44)).settlement()).isEqualTo(
                settlement("0", "200", "0", "0"));
        assertThat(atCutOff.cancelByCustomer(friday(21, 45)).settlement()).isEqualTo(
                settlement("45", "155", "0", "0"));
    }

    @Test
    @Tag("CCX-5")
    @DisplayName("Firm: free until Friday 9:45.")
    void firmTierIsFreeUntilTwentyFourHoursBeforeTheCheckInPeriod() {
        Reservation beforeCutOff = reservedAt(monday(22, 0), CancellationTier.FIRM);
        Reservation atCutOff = reservedAt(monday(22, 0), CancellationTier.FIRM);

        assertThat(beforeCutOff.cancelByCustomer(friday(9, 44)).settlement()).isEqualTo(
                settlement("0", "200", "0", "0"));
        assertThat(atCutOff.cancelByCustomer(friday(9, 45)).settlement()).isEqualTo(
                settlement("45", "155", "0", "0"));
    }

    @Test
    @Tag("CCX-6")
    @DisplayName("Aisyah cancels on Friday 22:00, pays RM45 and gets RM155 back.")
    void customerPaysTheBaseCancellationFeeAfterTheCutOffAndTheTimeOpensAgain() {
        Cancellation cancelled = reservation.cancelByCustomer(friday(22, 0));
        market.providerSchedule().release(reservation);
        market.scheduleOf(AISYAH_CUSTOMER_ID).release(reservation);

        assertThat(cancelled).isEqualTo(new Cancellation(friday(22, 0), CancellationCause.CUSTOMER_CANCELLED,
                settlement("45", "155", "0", "0")));
        assertThat(cancelled.settlement().providerPayoutGross()).isEqualTo(Money.myr(45));
        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(market.providerSchedule().blocks()).isEmpty();
        assertThat(market.scheduleOf(AISYAH_CUSTOMER_ID).blocks()).isEmpty();
        assertThat(market.book(meiBooking(), friday(22, 1), friday(22, 2)).status())
                .isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    @Tag("RES-8")
    @DisplayName("Kemas Home raises its rate to RM30 and switches to Firm on Wednesday; Aisyah's Saturday booking stays RM200 under Standard.")
    void cancellationReadsTheTierAndPriceTheReservationWasBookedWith() {
        market.providerBecomes(kemasHomeProvider().tier(CancellationTier.FIRM)
                .rate(Money.myr(30)).build());

        Cancellation cancelled = reservation.cancelByCustomer(friday(12, 0));

        assertThat(cancelled.settlement()).isEqualTo(settlement("0", "200", "0", "0"));
        assertThat(market.provider().terms().cancellationTier()).isEqualTo(CancellationTier.FIRM);
        assertThat(reservation.terms().cancellationTier()).isEqualTo(CancellationTier.STANDARD);
    }

    @Test
    @Tag("RES-8")
    @DisplayName("A reservation keeps Homa's reservation values from when it was Requested: Standard's cut-off was 24 hours then, so Friday 21:44 is late, whatever the values are now.")
    void cancellationReadsTheReservationValuesTheReservationWasRequestedWith() {
        Map<CancellationTier, Duration> oldCutOffs = Map.of(CancellationTier.FLEXIBLE, Duration.ofHours(4),
                CancellationTier.STANDARD, Duration.ofHours(24), CancellationTier.FIRM, Duration.ofHours(48));
        Version old = new Version(VALUES.effectiveFrom(), VALUES.checkInPeriod(), VALUES.protectionAfterJobEnd(),
                VALUES.acceptanceResponseTime(), VALUES.acceptanceCutOffBeforeCheckIn(), VALUES.gracePeriod(),
                VALUES.graceNeedsReservedBeforeCheckIn(), oldCutOffs, VALUES.providerFines(), VALUES.compensation(),
                VALUES.customerNoShowShareOfLaterHours(), VALUES.feeAndFineCap(), VALUES.partialTimeStep(),
                VALUES.completionRemindersAfterJobEnd());
        Reservation current = reservation;
        Reservation requestedUnderOldValues = Reservation.restore(current.id(), current.checkoutId(),
                current.booking(), current.terms(), current.quote(), old, current.requestedAt(),
                current.acceptanceDeadline().orElseThrow(), current.status(), current.reservedAt().orElse(null), null,
                null, null, null, null, null, List.of(), 0);

        assertThat(requestedUnderOldValues.cancelByCustomer(friday(21, 44)).settlement().customerFee())
                .isEqualTo(Money.myr(45));
        assertThat(current.cancelByCustomer(friday(21, 44)).settlement().customerFee()).isEqualTo(Money.myr(0));
    }

    // Customer: last minute (LMC-1, LMC-2)

    @Test
    @Tag("LMC-1")
    @DisplayName("Aisyah cancels at 9:50: she pays 45 + 0 + 20 = RM65 and gets RM135 back.")
    void customerCancelsInTheCheckInPeriodAndPaysBasePlusTravel() {
        Cancellation cancelled = reservation.cancelByCustomer(saturday(9, 50));

        assertThat(cancelled).isEqualTo(new Cancellation(saturday(9, 50), CancellationCause.CUSTOMER_LAST_MINUTE,
                settlement("65", "135", "0", "0")));
        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @Tag("LMC-1")
    @DisplayName("Last minute starts with the Check-in Period: at 9:44 Aisyah pays the Base Cancellation Fee only, at 9:45 she pays Base + travel.")
    void lastMinuteStartsExactlyWithTheCheckInPeriod() {
        Reservation beforeCheckIn = reservation;
        Reservation atCheckIn = Marketplace.withKemasHome().reserveStandard();

        Cancellation late = beforeCheckIn.cancelByCustomer(saturday(9, 44));
        Cancellation lastMinute = atCheckIn.cancelByCustomer(saturday(9, 45));

        assertThat(late.cause()).isEqualTo(CancellationCause.CUSTOMER_CANCELLED);
        assertThat(late.settlement()).isEqualTo(settlement("45", "155", "0", "0"));
        assertThat(lastMinute.cause()).isEqualTo(CancellationCause.CUSTOMER_LAST_MINUTE);
        assertThat(lastMinute.settlement()).isEqualTo(settlement("65", "135", "0", "0"));
    }

    @Test
    @Tag("LMC-2")
    @DisplayName("Aisyah stops the job at 11:20: 30 minutes are charged (RM22.50), so she pays RM87.50 and gets RM112.50 back.")
    void customerPaysTheHoursAfterTheFirstInQuarterHourStepsRoundedUp() {
        Cancellation cancelled = reservation.cancelByCustomer(saturday(11, 20));

        assertThat(cancelled.cause()).isEqualTo(CancellationCause.CUSTOMER_LAST_MINUTE);
        assertThat(cancelled.settlement()).isEqualTo(settlement("87.50", "112.50", "0", "0"));
    }

    @Test
    @Tag("LMC-2")
    @DisplayName("Stopping at 12:00 charges one hour after the first, RM45; at 12:05 the started quarter hour is charged too, RM56.25.")
    void aStartedQuarterHourIsChargedInFull() {
        Reservation atNoon = reservation;
        Reservation fiveMinutesLater = Marketplace.withKemasHome().reserveStandard();
        Reservation atTheStep = Marketplace.withKemasHome().reserveStandard();
        Reservation justPastTheStep = Marketplace.withKemasHome().reserveStandard();

        assertThat(atNoon.cancelByCustomer(saturday(12, 0)).settlement().customerFee()).isEqualTo(Money.myr(110));
        assertThat(fiveMinutesLater.cancelByCustomer(saturday(12, 5)).settlement().customerFee())
                .isEqualTo(Money.myr("121.25"));
        assertThat(atTheStep.cancelByCustomer(saturday(12, 15)).settlement().customerFee())
                .isEqualTo(Money.myr("121.25"));
        assertThat(justPastTheStep.cancelByCustomer(saturday(12, 16)).settlement().customerFee())
                .isEqualTo(Money.myr("132.50"));
    }

    @Test
    @Tag("LMC-2")
    @DisplayName("Nothing is charged for time before the job starts or in its first hour, which the Base Cancellation Fee covers: cancelling at 10:00 and at 11:00 both cost RM65.")
    void theFirstHourIsCoveredByTheBaseCancellationFee() {
        Reservation atStart = reservation;
        Reservation afterTheFirstHour = Marketplace.withKemasHome().reserveStandard();
        Reservation aMinuteIntoTheSecondHour = Marketplace.withKemasHome().reserveStandard();

        assertThat(atStart.cancelByCustomer(JOB_START).settlement().customerFee()).isEqualTo(Money.myr(65));
        assertThat(afterTheFirstHour.cancelByCustomer(saturday(11, 0)).settlement().customerFee())
                .isEqualTo(Money.myr(65));
        assertThat(aMinuteIntoTheSecondHour.cancelByCustomer(saturday(11, 1)).settlement().customerFee())
                .isEqualTo(Money.myr("76.25"));
    }

    @Test
    @Tag("LMC-2")
    @DisplayName("Aisyah stops the job at 13:59: three hours after the first are charged, so she pays Base + 135 + travel, the whole RM200, and nothing comes back.")
    void cancellingAMinuteBeforeTheJobEndCostsTheWholeTotalReservationFee() {
        Cancellation cancelled = reservation.cancelByCustomer(saturday(13, 59));

        assertThat(cancelled.settlement()).isEqualTo(settlement("200", "0", "0", "0"));
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("PAY-8 in a cancellation fee: at RM22.50 an hour, stopping at 11:05 costs 22.50 + 5.625 = RM28.125, rounded half up to RM28.13, and the refund is RM67.50 minus RM28.13, RM39.37.")
    void everyLineIsRoundedOnceToTheSenAndTheRefundIsThePaidAmountMinusTheRoundedFee() {
        Marketplace oddRate = new Marketplace(kemasHomeProvider().rate(Money.myr("22.50"))
                .travelFee(TravelFeeRule.none()).discount(DiscountRule.NONE).build());
        Reservation reserved = oddRate.book(aisyahBooking(JOB_START, Duration.ofHours(3), 1), monday(19, 58),
                monday(20, 0));
        reserved.accept(ACCEPTED_AT);

        Cancellation cancelled = reserved.cancelByCustomer(saturday(11, 5));

        assertThat(reserved.quote().total()).isEqualTo(Money.myr("67.50"));
        assertThat(cancelled.settlement().customerFee()).isEqualTo(Money.myr("28.13"));
        assertThat(cancelled.settlement().customerRefund()).isEqualTo(Money.myr("39.37"));
        assertThat(cancelled.settlement().customerFee().plus(cancelled.settlement().customerRefund()))
                .isEqualTo(reserved.quote().total());
    }

    // Preview

    @Test
    @Tag("CCX-6")
    @DisplayName("PreviewCustomerCancellation shows a RM45 fee. The Standard cut-off was Fri 21:45 and there is no grace period.")
    void previewShowsTheFeeWithoutCancelling() {
        Cancellation previewed = reservation.previewCustomerCancellation(friday(22, 0));

        assertThat(previewed).isEqualTo(new Cancellation(friday(22, 0), CancellationCause.CUSTOMER_CANCELLED,
                settlement("45", "155", "0", "0")));
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.cancellation()).isEmpty();
        assertThat(reservation.previewCustomerCancellation(friday(22, 0))).isEqualTo(previewed);
        assertThat(reservation.cancelByCustomer(friday(22, 0))).isEqualTo(previewed);
    }

    @Test
    @Tag("LMC-1")
    @DisplayName("The preview shows the last minute fee too, and the fee a minute earlier.")
    void previewFollowsTheMomentItIsAskedAt() {
        assertThat(reservation.previewCustomerCancellation(saturday(9, 44)).settlement().customerFee())
                .isEqualTo(Money.myr(45));
        assertThat(reservation.previewCustomerCancellation(saturday(9, 45)).settlement().customerFee())
                .isEqualTo(Money.myr(65));
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
    }

    // Provider (PCX-1, PCX-2, LMC-3)

    @Test
    @Tag("PCX-1")
    @DisplayName("Kemas Home cancels on Wednesday: fined RM45, Aisyah gets RM200.")
    void providerIsFinedTheBaseCancellationFeeMoreThan24HoursAhead() {
        Cancellation cancelled = reservation.cancelByProvider(wednesday(10, 0));

        assertThat(cancelled).isEqualTo(new Cancellation(wednesday(10, 0), CancellationCause.PROVIDER_CANCELLED,
                new Settlement(Money.myr(0), Money.myr(200), Money.myr(0), Money.myr(45), Money.myr(0))));
        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.cancellation()).contains(cancelled);
    }

    @Test
    @Tag("PCX-2")
    @DisplayName("Kemas Home cancels on Friday 20:00: fined RM65, Aisyah gets RM206.")
    void providerIsFinedBasePlusTenPercentWithin24HoursAndTheCustomerGetsThreePercentMore() {
        Cancellation cancelled = reservation.cancelByProvider(friday(20, 0));

        assertThat(cancelled).isEqualTo(new Cancellation(friday(20, 0), CancellationCause.PROVIDER_CANCELLED,
                new Settlement(Money.myr(0), Money.myr(200), Money.myr(6), Money.myr(65), Money.myr(0))));
    }

    @Test
    @Tag("PCX-2")
    @DisplayName("More than 24 hours ahead means Friday 9:44 or earlier: at 9:45, exactly 24 hours before the Check-in Period, Kemas Home is fined Base + 10%.")
    void twentyFourHoursAheadIsWithinTwentyFourHours() {
        Reservation justOver = reservation;
        Reservation exactly = Marketplace.withKemasHome().reserveStandard();

        assertThat(justOver.cancelByProvider(friday(9, 44)).settlement().providerFine()).isEqualTo(Money.myr(45));
        assertThat(exactly.cancelByProvider(friday(9, 45)).settlement().providerFine()).isEqualTo(Money.myr(65));
    }

    @Test
    @Tag("PCX-1")
    @DisplayName("The Grace Period is the customer's: Kemas Home cancelling on Monday 23:00, inside it, is still fined RM45.")
    void providerIsFinedEvenInsideTheCustomersGracePeriod() {
        Cancellation cancelled = reservation.cancelByProvider(monday(23, 0));

        assertThat(cancelled.settlement().providerFine()).isEqualTo(Money.myr(45));
        assertThat(cancelled.cause()).isEqualTo(CancellationCause.PROVIDER_CANCELLED);
    }

    @Test
    @Tag("LMC-3")
    @DisplayName("Kemas Home's van breaks down at 9:50: fined RM85, Aisyah gets RM214.")
    void providerIsFinedBasePlusTwentyPercentInTheCheckInPeriodAndTheCustomerGetsSevenPercentMore() {
        Cancellation cancelled = reservation.cancelByProvider(saturday(9, 50));

        assertThat(cancelled).isEqualTo(new Cancellation(saturday(9, 50), CancellationCause.PROVIDER_LAST_MINUTE,
                new Settlement(Money.myr(0), Money.myr(200), Money.myr(14), Money.myr(85), Money.myr(0))));
        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @Tag("LMC-3")
    @DisplayName("Last minute starts with the Check-in Period for the provider too, and runs to the job end: 9:44 is PCX-2, 9:45 and 13:59 are LMC-3.")
    void providerLastMinuteRunsFromTheCheckInPeriodToTheJobEnd() {
        Reservation beforeCheckIn = reservation;
        Reservation atCheckIn = Marketplace.withKemasHome().reserveStandard();
        Reservation inTheJob = Marketplace.withKemasHome().reserveStandard();

        assertThat(beforeCheckIn.cancelByProvider(saturday(9, 44)).cause()).isEqualTo(CancellationCause.PROVIDER_CANCELLED);
        assertThat(atCheckIn.cancelByProvider(saturday(9, 45)).cause()).isEqualTo(CancellationCause.PROVIDER_LAST_MINUTE);
        assertThat(inTheJob.cancelByProvider(saturday(13, 59)).settlement().providerFine()).isEqualTo(Money.myr(85));
    }

    @Test
    @Tag("CAN-2")
    @DisplayName("For a RM25 job, Base + 10% would be 25 + 2.50 = RM27.50 and Base + 20% RM30; both are capped at RM25.")
    void providerFineIsCappedAtTheTotalReservationFee() {
        Reservation within24Hours = cheapReservation();
        Reservation lastMinute = cheapReservation();

        Settlement late = within24Hours.cancelByProvider(friday(20, 0)).settlement();
        Settlement atCheckIn = lastMinute.cancelByProvider(saturday(9, 50)).settlement();

        assertThat(late).isEqualTo(new Settlement(Money.myr(0), Money.myr(25), Money.myr("0.75"), Money.myr(25),
                Money.myr(0)));
        assertThat(atCheckIn).isEqualTo(new Settlement(Money.myr(0), Money.myr(25), Money.myr("1.75"), Money.myr(25),
                Money.myr(0)));
    }

    /** A RM25 job: one helper for one hour at RM25, no travel fee and no discount. */
    private static Reservation cheapReservation() {
        BookingBounds fromOneHour = new BookingBounds(Duration.ofHours(1), Duration.ofHours(6),
                Duration.ofMinutes(15), 1, Integer.MAX_VALUE);
        Marketplace cheap = new Marketplace(kemasHomeProvider().rate(Money.myr(25)).travelFee(TravelFeeRule.none())
                .discount(DiscountRule.NONE).bounds(fromOneHour).build());
        Reservation reserved = cheap.book(aisyahBooking(JOB_START, Duration.ofHours(1), 1), monday(19, 58),
                monday(20, 0));
        reserved.accept(ACCEPTED_AT);
        return reserved;
    }

    // What can be cancelled, and when

    @Test
    @Tag("REQ-1")
    @DisplayName("A Requested reservation is withdrawn, not cancelled: neither party can cancel it, and nothing changes.")
    void aRequestedReservationCannotBeCancelled() {
        Reservation requested = Marketplace.withKemasHome().requestStandard();

        assertThatThrownBy(() -> requested.cancelByCustomer(monday(21, 0)))
                .isInstanceOf(ReservationStatusConflictException.class);
        assertThatThrownBy(() -> requested.cancelByProvider(monday(21, 0)))
                .isInstanceOf(ReservationStatusConflictException.class);
        assertThatThrownBy(() -> requested.previewCustomerCancellation(monday(21, 0)))
                .isInstanceOf(ReservationStatusConflictException.class);
        assertThat(requested.status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(requested.cancellation()).isEmpty();
    }

    @Test
    @Tag("CCX-6")
    @DisplayName("A cancelled reservation can't be cancelled again by either party, and keeps its first settlement.")
    void aCancelledReservationCannotBeCancelledAgain() {
        Cancellation first = reservation.cancelByCustomer(friday(22, 0));

        assertThatThrownBy(() -> reservation.cancelByCustomer(friday(22, 1)))
                .isInstanceOf(ReservationStatusConflictException.class);
        assertThatThrownBy(() -> reservation.cancelByProvider(friday(22, 1)))
                .isInstanceOf(ReservationStatusConflictException.class);
        assertThat(reservation.cancellation()).contains(first);
    }

    @Test
    @Tag("LMC-1")
    @DisplayName("The job ended at 14:00, so neither party can cancel it any more; a Completed job can't be cancelled either.")
    void aJobThatEndedCannotBeCancelled() {
        Reservation completed = Marketplace.withKemasHome().reserveStandard();
        completed.markCompleted(List.of(new CompletionPhoto("photo-clean-kitchen")), JOB_END);

        assertThatThrownBy(() -> reservation.cancelByCustomer(JOB_END)).isInstanceOf(JobAlreadyEndedException.class);
        assertThatThrownBy(() -> reservation.cancelByProvider(JOB_END)).isInstanceOf(JobAlreadyEndedException.class);
        assertThatThrownBy(() -> reservation.previewCustomerCancellation(saturday(15, 0)))
                .isInstanceOf(JobAlreadyEndedException.class);
        assertThatThrownBy(() -> completed.cancelByCustomer(saturday(15, 0)))
                .isInstanceOf(ReservationStatusConflictException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.cancellation()).isEmpty();
    }

    // The values a settlement reads are the reservation's own (P3)

    @Test
    @Tag("RES-8")
    @DisplayName("A reservation keeps Homa's fines and compensation from when it was Requested, whatever they are now.")
    void providerCancellationReadsTheFinesTheReservationWasRequestedWith() {
        ProviderFines oldFines = new ProviderFines(Duration.ofHours(48), Percentage.of(15), Percentage.of(12),
                Percentage.of(25), Percentage.of(30));
        CustomerCompensation oldCompensation = new CustomerCompensation(Percentage.of(5), Percentage.of(4),
                Percentage.of(8), Percentage.of(10));
        Version old = new Version(VALUES.effectiveFrom(), VALUES.checkInPeriod(), VALUES.protectionAfterJobEnd(),
                VALUES.acceptanceResponseTime(), VALUES.acceptanceCutOffBeforeCheckIn(), VALUES.gracePeriod(),
                VALUES.graceNeedsReservedBeforeCheckIn(), VALUES.freeCancellationBeforeCheckIn(), oldFines,
                oldCompensation, VALUES.customerNoShowShareOfLaterHours(), VALUES.feeAndFineCap(),
                VALUES.partialTimeStep(), VALUES.completionRemindersAfterJobEnd());
        Reservation current = reservation;
        Reservation requestedUnderOldValues = restoredWith(current, old);
        Reservation requestedUnderOldValuesAtCheckIn = restoredWith(current, old);

        Settlement within48Hours = requestedUnderOldValues.cancelByProvider(thursday(10, 0)).settlement();
        Settlement lastMinute = requestedUnderOldValuesAtCheckIn.cancelByProvider(saturday(9, 50)).settlement();

        assertThat(within48Hours).isEqualTo(
                new Settlement(Money.myr(0), Money.myr(200), Money.myr(10), Money.myr(75), Money.myr(0)));
        assertThat(lastMinute).isEqualTo(
                new Settlement(Money.myr(0), Money.myr(200), Money.myr(16), Money.myr(95), Money.myr(0)));
        assertThat(current.cancelByProvider(thursday(10, 0)).settlement().providerFine()).isEqualTo(Money.myr(45));
    }

    private static Reservation restoredWith(Reservation from, Version values) {
        return Reservation.restore(from.id(), from.checkoutId(), from.booking(), from.terms(), from.quote(), values,
                from.requestedAt(), from.acceptanceDeadline().orElseThrow(), from.status(),
                from.reservedAt().orElse(null), null, null, null, null, null, null, List.of(), 0);
    }
}
