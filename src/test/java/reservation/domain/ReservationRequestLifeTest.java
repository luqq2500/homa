package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.support.ReservationFixtures.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import kernel.Money;
import kernel.TimeRange;
import org.junit.jupiter.api.Test;
import reservation.domain.exception.CannotCancelDuringCheckInPeriodException;
import reservation.domain.exception.CompletionAlreadySubmittedException;
import reservation.domain.exception.InstantBookCannotBeDeclinedException;
import reservation.domain.exception.InvalidHelpersException;
import reservation.domain.exception.PaymentNotSecuredException;
import reservation.domain.exception.ProviderNotVerifiedForJobException;
import reservation.domain.exception.RequestExpiredException;
import reservation.domain.exception.ReservationStatusException;
import reservation.domain.exception.TimeNotFreeException;

class ReservationRequestLifeTest {

    private static final ProviderTerms INSTANT = terms(CancellationTier.STANDARD, AcceptancePolicy.INSTANT_BOOK);
    private static final Instant FEE_WINDOW_OPENS = START.minus(Duration.ofHours(24));

    @Test
    void aRequestExpiresTwentyFourHoursAfterItWasFirstSubmitted() {
        Reservation reservation = secured(terms());

        assertThat(reservation.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
        assertThat(reservation.expire(NOW.plus(Duration.ofHours(24)).minusSeconds(1))).isFalse();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.REQUESTED);

        assertThat(reservation.expire(NOW.plus(Duration.ofHours(24)))).isTrue();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(reservation.ending()).contains(Ending.EXPIRED);
        assertThat(reservation.settlement()).contains(Settlement.fullRefund());
        assertThat(reservation.expire(NOW.plus(Duration.ofHours(25)))).isFalse();
    }

    @Test
    void aRequestForASoonStartExpiresAtTheResponseDeadlineInstead() {
        Instant soon = NOW.plus(Duration.ofHours(3));
        Reservation reservation = Reservation.request(customer(), terms(), VERIFIED, freeSchedule(),
                new BookingDetails(Set.of(KITCHEN), soon, 3, 2), VALUES, NOW);

        assertThat(reservation.expiresAt()).isEqualTo(soon.minus(Duration.ofHours(1)));
    }

    @Test
    void theProviderSeesAndAcceptsARequestOnlyOncePaymentIsSecured() {
        Reservation reservation = requested();

        assertThatThrownBy(() -> reservation.accept(VERIFIED, freeSchedule(), NOW)).isInstanceOf(PaymentNotSecuredException.class);
        assertThatThrownBy(() -> reservation.decline(NOW)).isInstanceOf(PaymentNotSecuredException.class);

        reservation.securePayment();
        assertThat(reservation.isPaymentSecured()).isTrue();
        reservation.accept(VERIFIED, freeSchedule(), NOW.plusSeconds(60));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.state().acceptedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void aFailedPaymentEndsTheRequestStraightAway() {
        Reservation reservation = requested();

        reservation.failPayment();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.ending()).contains(Ending.PAYMENT_FAILED);
        assertThat(reservation.settlement()).contains(Settlement.fullRefund());
        assertThatThrownBy(reservation::failPayment).isInstanceOf(ReservationStatusException.class);
        assertThatThrownBy(reservation::securePayment).isInstanceOf(ReservationStatusException.class);
    }

    @Test
    void theProviderCanAnswerUpToTheMomentTheRequestExpires() {
        Instant expiry = NOW.plus(Duration.ofHours(24));

        secured(terms()).accept(VERIFIED, freeSchedule(), expiry.minusSeconds(1));
        assertThatThrownBy(() -> secured(terms()).accept(VERIFIED, freeSchedule(), expiry))
                .isInstanceOf(RequestExpiredException.class);
        assertThatThrownBy(() -> secured(terms()).decline(expiry)).isInstanceOf(RequestExpiredException.class);
    }

    @Test
    void theProviderAcceptsOnlyATimeTheyAreFreeAndVerifiedFor() {
        Reservation reservation = secured(terms());
        ProviderSchedule busy = schedule(List.of(TimeRange.of(START, END)));

        assertThatThrownBy(() -> reservation.accept(VERIFIED, busy, NOW)).isInstanceOf(TimeNotFreeException.class);
        assertThatThrownBy(() -> reservation.accept(ProviderStanding.unverified(PROVIDER), freeSchedule(), NOW))
                .isInstanceOf(ProviderNotVerifiedForJobException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    void aDeclinedRequestCostsTheCustomerNothing() {
        Reservation reservation = secured(terms());

        reservation.decline(NOW);

        assertThat(reservation.status()).isEqualTo(ReservationStatus.DECLINED);
        assertThat(reservation.ending()).contains(Ending.DECLINED_BY_PROVIDER);
        assertThat(reservation.settlement()).contains(Settlement.fullRefund());
        assertThatThrownBy(() -> reservation.accept(VERIFIED, freeSchedule(), NOW)).isInstanceOf(ReservationStatusException.class);
    }

    @Test
    void anInstantBookRequestIsAcceptedTheMomentItsPaymentIsSecured() {
        Reservation reservation = secured(INSTANT);

        assertThat(reservation.acceptInstantly(VERIFIED, freeSchedule(), NOW)).isTrue();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.state().acceptedAt()).isEqualTo(NOW);
    }

    @Test
    void instantBookNeverAcceptsATimeTheProviderIsNoLongerFree() {
        Reservation taken = secured(INSTANT);
        Reservation unverified = secured(INSTANT);

        assertThat(taken.acceptInstantly(VERIFIED, schedule(List.of(TimeRange.of(START, END))), NOW)).isFalse();
        assertThat(unverified.acceptInstantly(ProviderStanding.unverified(PROVIDER), freeSchedule(), NOW)).isFalse();

        assertThat(taken.status()).isEqualTo(ReservationStatus.DECLINED);
        assertThat(taken.ending()).contains(Ending.TIME_NO_LONGER_FREE);
        assertThat(taken.settlement()).contains(Settlement.fullRefund());
        assertThat(unverified.status()).isEqualTo(ReservationStatus.DECLINED);
        assertThat(taken.state().acceptedAt()).isNull();
    }

    @Test
    void theProviderCannotDeclineAnInstantBookRequest() {
        Reservation reservation = secured(INSTANT);

        assertThatThrownBy(() -> reservation.decline(NOW)).isInstanceOf(InstantBookCannotBeDeclinedException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    void anOpenRequestThatClashesWithAnAcceptedBookingOrItsBufferIsDeclinedAtOnce() {
        Duration buffer = Duration.ofMinutes(30);
        Reservation overlapping = secured(terms());
        Reservation insideTheBuffer = secured(terms());
        Reservation clear = secured(terms());

        assertThat(overlapping.declineIfClashingWith(TimeRange.of(START.plus(Duration.ofHours(1)), END), buffer)).isTrue();
        assertThat(insideTheBuffer.declineIfClashingWith(
                TimeRange.of(END.plus(Duration.ofMinutes(15)), END.plus(Duration.ofHours(2))), buffer)).isTrue();
        assertThat(clear.declineIfClashingWith(
                TimeRange.of(END.plus(Duration.ofMinutes(30)), END.plus(Duration.ofHours(2))), buffer)).isFalse();

        assertThat(overlapping.status()).isEqualTo(ReservationStatus.DECLINED);
        assertThat(overlapping.ending()).contains(Ending.DECLINED_FOR_CLASH);
        assertThat(overlapping.settlement()).contains(Settlement.fullRefund());
        assertThat(clear.status()).isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    void anAcceptedReservationIsNeverDeclinedForAClash() {
        Reservation reserved = reserved();

        assertThat(reserved.declineIfClashingWith(TimeRange.of(START, END), Duration.ZERO)).isFalse();
        assertThat(reserved.status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void theCustomerEditsARequestUntilTheProviderAccepts() {
        Reservation reservation = secured(terms());
        BookingDetails fewerHours = new BookingDetails(Set.of(KITCHEN, LAUNDRY), START.plus(Duration.ofHours(1)), 2, 2);

        reservation.edit(fewerHours, VERIFIED, freeSchedule(), NOW.plusSeconds(60));

        assertThat(reservation.details()).isEqualTo(fewerHours);
        assertThat(reservation.price().hoursFee()).isEqualTo(Money.ofRinggit(120));
        assertThat(reservation.state().address()).isEqualTo(HOME);
        assertThatThrownBy(() -> reserved().edit(fewerHours, VERIFIED, freeSchedule(), NOW))
                .isInstanceOf(ReservationStatusException.class);
    }

    @Test
    void anEditThatChangesTheTotalMustBeSecuredAgainBeforeTheProviderCanAccept() {
        Reservation reservation = secured(terms());

        reservation.edit(new BookingDetails(Set.of(KITCHEN), START, 3, 3), VERIFIED, freeSchedule(), NOW);

        assertThat(reservation.isPaymentSecured()).isFalse();
        assertThatThrownBy(() -> reservation.accept(VERIFIED, freeSchedule(), NOW)).isInstanceOf(PaymentNotSecuredException.class);
    }

    @Test
    void anEditThatKeepsTheTotalKeepsThePaymentSecured() {
        Reservation reservation = secured(terms());

        reservation.edit(new BookingDetails(Set.of(LAUNDRY), START.plus(Duration.ofHours(2)), 3, 2), VERIFIED, freeSchedule(), NOW);

        assertThat(reservation.isPaymentSecured()).isTrue();
    }

    @Test
    void anEditIsCheckedLikeANewRequestAndARefusedOneChangesNothing() {
        Reservation reservation = secured(terms());

        assertThatThrownBy(() -> reservation.edit(new BookingDetails(Set.of(KITCHEN), START, 3, 4), VERIFIED, freeSchedule(), NOW))
                .isInstanceOf(InvalidHelpersException.class);
        assertThatThrownBy(() -> reservation.edit(new BookingDetails(Set.of(KITCHEN), START, 3, 1), VERIFIED,
                schedule(List.of(TimeRange.of(START, END))), NOW)).isInstanceOf(TimeNotFreeException.class);
        assertThat(reservation.details()).isEqualTo(DETAILS);
        assertThat(reservation.isPaymentSecured()).isTrue();
    }

    @Test
    void editingDoesNotRestartTheExpiry() {
        Reservation reservation = secured(terms());

        reservation.edit(new BookingDetails(Set.of(KITCHEN), START.plus(Duration.ofDays(2)), 3, 2), VERIFIED,
                freeSchedule(), NOW.plus(Duration.ofHours(20)));

        assertThat(reservation.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
        assertThatThrownBy(() -> reservation.edit(DETAILS, VERIFIED, freeSchedule(), NOW.plus(Duration.ofHours(24))))
                .isInstanceOf(RequestExpiredException.class);
    }

    @Test
    void anEditThatMovesTheStartEarlierBringsTheExpiryToTheResponseDeadline() {
        Reservation reservation = secured(terms());
        Instant soon = NOW.plus(Duration.ofHours(4));

        reservation.edit(new BookingDetails(Set.of(KITCHEN), soon, 3, 2), VERIFIED, freeSchedule(), NOW);

        assertThat(reservation.expiresAt()).isEqualTo(soon.minus(Duration.ofHours(1)));
    }

    @Test
    void theCustomerCancelsARequestForFreeBeforeTheProviderResponds() {
        Reservation reservation = secured(terms());

        reservation.cancelByCustomer(START.minusSeconds(1));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.ending()).contains(Ending.CANCELLED_BY_CUSTOMER);
        assertThat(reservation.settlement()).contains(Settlement.fullRefund());
    }

    @Test
    void cancellingBeforeTheFeeWindowOpensIsAFullRefund() {
        Reservation reservation = reserved(terms(CancellationTier.FIRM, AcceptancePolicy.REQUEST_AND_ACCEPT));
        assertThat(reservation.feeWindowOpensAt()).isEqualTo(FEE_WINDOW_OPENS);

        reservation.cancelByCustomer(FEE_WINDOW_OPENS.minusSeconds(1));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.settlement()).contains(Settlement.fullRefund());
        assertThat(reservation.fine()).isEmpty();
    }

    @Test
    void standardCostsTenPercentOfTheHoursFeeInsideTheFeeWindowAndTheTravelFeeIsRefunded() {
        Reservation reservation = reserved(terms(CancellationTier.STANDARD, AcceptancePolicy.REQUEST_AND_ACCEPT));

        reservation.cancelByCustomer(FEE_WINDOW_OPENS);

        assertThat(reservation.settlement()).contains(new Settlement(Money.ofRinggit(18), PayoutKind.CANCELLATION_FEE));
    }

    @Test
    void firmCostsTwentyPercentOfTheHoursFeeInsideTheFeeWindow() {
        Reservation reservation = reserved(terms(CancellationTier.FIRM, AcceptancePolicy.REQUEST_AND_ACCEPT));

        reservation.cancelByCustomer(START.minusSeconds(1));

        assertThat(reservation.settlement()).contains(new Settlement(Money.ofRinggit(36), PayoutKind.CANCELLATION_FEE));
    }

    @Test
    void flexibleIsFreeUntilTheBookedStart() {
        Reservation reservation = reserved(terms(CancellationTier.FLEXIBLE, AcceptancePolicy.REQUEST_AND_ACCEPT));

        reservation.cancelByCustomer(START.minusSeconds(1));

        assertThat(reservation.settlement()).contains(Settlement.fullRefund());
    }

    @Test
    void duringTheCheckInPeriodTheCustomerCannotCancelUntilTheProviderChecksIn() {
        Reservation reservation = teamNamed();

        assertThatThrownBy(() -> reservation.cancelByCustomer(START)).isInstanceOf(CannotCancelDuringCheckInPeriodException.class);
        assertThatThrownBy(() -> reservation.cancelByCustomer(START.plus(Duration.ofMinutes(15))))
                .isInstanceOf(CannotCancelDuringCheckInPeriodException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void cancellingAfterCheckInCostsTheCustomerNoShowFeeEvenBeforeTheBookedStart() {
        Reservation reservation = teamNamed();
        reservation.checkIn("helper-1", FaceCheckResult.PASSED, true, START.minus(Duration.ofMinutes(10)));

        reservation.cancelByCustomer(START.minus(Duration.ofMinutes(5)));

        assertThat(reservation.customerNoShowFee()).isEqualTo(Money.ofRinggit(64));
        assertThat(reservation.settlement()).contains(new Settlement(Money.ofRinggit(64), PayoutKind.CUSTOMER_NO_SHOW_FEE));
        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void aProviderWhoCancelsBeforeTheFeeWindowPaysNoFineAndTheCustomerIsRefundedInFull() {
        Reservation reservation = reserved();

        reservation.cancelByProvider(FEE_WINDOW_OPENS.minusSeconds(1));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.ending()).contains(Ending.CANCELLED_BY_PROVIDER);
        assertThat(reservation.settlement()).contains(Settlement.fullRefund());
        assertThat(reservation.fine()).isEmpty();
    }

    @Test
    void aProviderWhoCancelsInsideTheFeeWindowIsFinedTwentyPercentOfTheHoursFee() {
        Reservation atTheOpening = reserved();
        Reservation justBeforeStart = reserved();

        atTheOpening.cancelByProvider(FEE_WINDOW_OPENS);
        justBeforeStart.cancelByProvider(START.minusSeconds(1));

        assertThat(atTheOpening.fine()).contains(new Fine(Money.ofRinggit(36), FineReason.LATE_CANCELLATION));
        assertThat(justBeforeStart.fine()).contains(new Fine(Money.ofRinggit(36), FineReason.LATE_CANCELLATION));
        assertThat(atTheOpening.settlement()).contains(Settlement.fullRefund());
    }

    @Test
    void aProviderWhoCancelsAfterTheBookedStartOrAfterCheckInIsFinedThirtyPercent() {
        Reservation afterStart = reserved();
        Reservation afterEarlyCheckIn = teamNamed();
        afterEarlyCheckIn.checkIn("helper-1", FaceCheckResult.PASSED, true, START.minus(Duration.ofMinutes(10)));

        afterStart.cancelByProvider(START);
        afterEarlyCheckIn.cancelByProvider(START.minus(Duration.ofMinutes(5)));

        assertThat(afterStart.fine()).contains(new Fine(Money.ofRinggit(54), FineReason.NO_SHOW));
        assertThat(afterEarlyCheckIn.fine()).contains(new Fine(Money.ofRinggit(54), FineReason.NO_SHOW));
    }

    @Test
    void onceCompletionIsSubmittedNeitherPartyCanCancel() {
        Reservation reservation = completionSubmitted();

        assertThatThrownBy(() -> reservation.cancelByCustomer(END.plusSeconds(1))).isInstanceOf(CompletionAlreadySubmittedException.class);
        assertThatThrownBy(() -> reservation.cancelByProvider(END.plusSeconds(1))).isInstanceOf(CompletionAlreadySubmittedException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void anEndedReservationCannotBeCancelled() {
        Reservation reservation = reserved();
        reservation.cancelByProvider(NOW);

        assertThatThrownBy(() -> reservation.cancelByCustomer(NOW)).isInstanceOf(ReservationStatusException.class);
        assertThatThrownBy(() -> reservation.cancelByProvider(NOW)).isInstanceOf(ReservationStatusException.class);
        assertThatThrownBy(() -> requested().cancelByProvider(NOW)).isInstanceOf(ReservationStatusException.class);
    }
}
