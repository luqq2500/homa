package com.homa.payment.usecase.ie;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_NAME;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ACCOUNT_NUMBER;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.BANK;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.COMPLETED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.KEMAS_HOMES_ACCOUNT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ONLINE_BANKING;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PAID_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PAYOUT_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PROTECTION_WINDOW_END;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVATION;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVATION_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.SMALLER_ROSTER_ACCEPTED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.SMALLER_ROSTER_ADJUSTMENT_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.adjusted;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.aisyahsRequestedHold;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.kemasHomesPayoutAtReserved;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.kemasHomesVerifiedAccount;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.smallerRosterAccepted;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.PayoutReleasedIE;
import com.homa.ie.events.RefundPaidIE;
import com.homa.kernel.Money;
import com.homa.payment.domain.LedgerAccount;
import com.homa.payment.domain.LedgerEntry;
import com.homa.payment.domain.LedgerMovement;
import com.homa.payment.domain.PaymentHold;
import com.homa.payment.domain.PaymentHoldStatus;
import com.homa.payment.domain.Payout;
import com.homa.payment.domain.PayoutKind;
import com.homa.payment.domain.PayoutStatus;
import com.homa.payment.domain.exception.AmountNotPayableException;
import com.homa.payment.domain.exception.PayoutNotPendingException;
import com.homa.payment.domain.exception.RefundExceedsHeldAmountException;
import com.homa.payment.usecase.FixedTimeSource;
import com.homa.payment.usecase.InMemoryFineAccountRepository;
import com.homa.payment.usecase.InMemoryLedgerRepository;
import com.homa.payment.usecase.InMemoryPaymentHoldRepository;
import com.homa.payment.usecase.InMemoryPayoutAccountRepository;
import com.homa.payment.usecase.InMemoryPayoutRepository;
import com.homa.payment.usecase.InMemoryProcessedMessageStore;
import com.homa.payment.usecase.RecordingPaymentGateway;
import com.homa.payment.usecase.RecordingPaymentIEPublisher;
import com.homa.payment.usecase.RecordingPayoutGateway;
import com.homa.payment.usecase.ReleaseDuePayouts;
import com.homa.payment.usecase.SequentialEventIds;
import com.homa.payment.usecase.dto.ReleaseDuePayoutsRequest;
import com.homa.payment.usecase.exception.AdjustedTotalMismatchException;
import com.homa.payment.usecase.exception.PayoutNotFoundException;
import com.homa.payment.usecase.exception.ReservationHoldNotFoundException;
import com.homa.payment.usecase.exception.UnknownAdjustmentReasonException;
import com.homa.payment.usecase.port.out.PaymentGateway;
import com.homa.payment.usecase.port.out.PayoutGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleSettlementAdjustedIETest {

    private static final String SMALLER_ROSTER_REFUND_KEY = "adjustment:adjustment-smaller-roster:refund";

    private final InMemoryPaymentHoldRepository holds = new InMemoryPaymentHoldRepository();
    private final InMemoryPayoutRepository payouts = new InMemoryPayoutRepository();
    private final RecordingPaymentGateway gateway = new RecordingPaymentGateway();
    private final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();
    private final RecordingPaymentIEPublisher publisher = new RecordingPaymentIEPublisher();
    private final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    private final FixedTimeSource time = new FixedTimeSource(SMALLER_ROSTER_ACCEPTED_AT);
    private final SequentialEventIds eventIds = new SequentialEventIds();
    private final HandleSettlementAdjustedIE handler = new HandleSettlementAdjustedIE(holds, payouts, gateway, ledger,
            publisher, processed, time, eventIds);

    /** Aisyah's RM200 is held and Kemas Home's payout is open, as they are once the reservation is Reserved. */
    private PaymentHold givenAisyahPaidAndReserved() {
        PaymentHold hold = aisyahsRequestedHold();
        holds.given(hold);
        payouts.given(kemasHomesPayoutAtReserved());
        ledger.post(new LedgerEntry(LedgerMovement.PAYMENT_HELD, Money.myr(200), PAID_AT));
        return hold;
    }

    private Payout payoutOfTheReservation() {
        return payouts.findByReservationId(RESERVATION).orElseThrow();
    }

    private void assertNothingWasSaidOrSent() {
        assertThat(gateway.refunds()).isEmpty();
        assertThat(publisher.published()).isEmpty();
        assertThat(holds.saves()).isEmpty();
        assertThat(payouts.saves()).isEmpty();
        assertThat(ledger.entries()).extracting(LedgerEntry::movement).doesNotContain(LedgerMovement.REFUND_SENT);
    }

    @Test
    @Tag("ROS-8")
    @Tag("PAY-7")
    @DisplayName("Kemas Home offers Siti alone. Aisyah accepts: 25 × 4 × 1 × 0.9 + 20 = RM110, so she gets RM90 back, "
            + "sent at once from her held RM200.")
    void theDifferenceOfASmallerRosterIsRefundedAtOnce() {
        PaymentHold hold = givenAisyahPaidAndReserved();

        handler.handle(smallerRosterAccepted("reservation-event-1"));

        assertThat(gateway.refunds()).containsExactly(new PaymentGateway.RefundRequest(SMALLER_ROSTER_REFUND_KEY,
                ONLINE_BANKING.value(), Money.myr(90)));
        assertThat(gateway.compensations()).isEmpty();
        assertThat(hold.held()).isEqualTo(Money.myr(110));
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.HELD);
        assertThat(holds.saves()).containsExactly(hold);
        assertThat(processed.recorded()).containsExactly("reservation-event-1|payment.SettlementAdjusted");
    }

    @Test
    @Tag("ROS-8")
    @Tag("PAY-7")
    @DisplayName("Aisyah's RM90 is sent on Friday 18:30: the event says so, field by field, and the ledger shows RM110 "
            + "still held.")
    void refundPaidIsPublishedAndTheLedgerPosted() {
        givenAisyahPaidAndReserved();

        handler.handle(smallerRosterAccepted("reservation-event-1"));

        assertThat(publisher.published()).containsExactly(new RefundPaidIE("event-1", RESERVATION_ID,
                AISYAH_CUSTOMER_ID, Money.myr(90), "SMALLER_ROSTER", SMALLER_ROSTER_ACCEPTED_AT));
        assertThat(ledger.entries()).last().isEqualTo(
                new LedgerEntry(LedgerMovement.REFUND_SENT, Money.myr(90), SMALLER_ROSTER_ACCEPTED_AT));
        assertThat(ledger.balanceOf(LedgerAccount.CUSTOMER_HELD)).isEqualTo(Money.myr(110));
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("With Siti alone, Kemas Home's payout drops from RM200 to RM110; it is still a job payout, waiting for "
            + "the job and the Protection Window.")
    void theProvidersPayoutDropsByTheRefund() {
        givenAisyahPaidAndReserved();

        handler.handle(smallerRosterAccepted("reservation-event-1"));

        Payout payout = payoutOfTheReservation();
        assertThat(payout.gross()).isEqualTo(Money.myr(110));
        assertThat(payout.kind()).isEqualTo(PayoutKind.JOB);
        assertThat(payout.status()).isEqualTo(PayoutStatus.PENDING);
        assertThat(payout.protectionWindowEnd()).isEqualTo(PROTECTION_WINDOW_END);
        assertThat(payouts.saves()).containsExactly(payout);
    }

    @Test
    @Tag("ROS-8")
    @Tag("PAY-7")
    @DisplayName("Kemas Home's payout drops to RM110; at release it pays the 15.5% platform fee on RM110 (RM17.05), so "
            + "RM92.95 is sent, and nothing is left held for Aisyah.")
    void theReleaseTakesThePlatformFeeOnWhatTheReservationNowCosts() {
        givenAisyahPaidAndReserved();
        handler.handle(smallerRosterAccepted("reservation-event-1"));
        payoutOfTheReservation().markCompleted(COMPLETED_AT);
        InMemoryPayoutAccountRepository accounts = new InMemoryPayoutAccountRepository();
        accounts.given(kemasHomesVerifiedAccount());
        RecordingPayoutGateway payoutGateway = new RecordingPayoutGateway();
        time.set(PROTECTION_WINDOW_END);

        new ReleaseDuePayouts(payouts, accounts, new InMemoryFineAccountRepository(), payoutGateway, ledger,
                publisher, time, eventIds).releaseDue(new ReleaseDuePayoutsRequest());

        assertThat(payoutGateway.transfers()).containsExactly(new PayoutGateway.Transfer("payout:" + PAYOUT_ID,
                KEMAS_HOMES_ACCOUNT.value(), BANK, ACCOUNT_NUMBER, KEMAS_HOME_NAME, Money.myr("92.95")));
        assertThat(publisher.published()).last().isEqualTo(new PayoutReleasedIE("event-2", PAYOUT_ID, RESERVATION_ID,
                KEMAS_HOME_PROVIDER_ID, "JOB", Money.myr(110), Money.myr("17.05"), Money.myr(0), Money.myr("92.95"),
                PROTECTION_WINDOW_END));
        assertThat(ledger.balanceOf(LedgerAccount.CUSTOMER_HELD)).isEqualTo(Money.myr(0));
        assertThat(ledger.balanceOf(LedgerAccount.PROVIDER_PAYABLE)).isEqualTo(Money.myr(0));
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_REVENUE)).isEqualTo(Money.myr("17.05"));
    }

    @Test
    @Tag("ROS-8")
    @Tag("PAY-7")
    @DisplayName("A second adjustment of the same reservation has its own id: it refunds again with its own key, and "
            + "lowers the payout again.")
    void aSecondAdjustmentWithANewIdRefundsAgain() {
        PaymentHold hold = givenAisyahPaidAndReserved();
        handler.handle(smallerRosterAccepted("reservation-event-1"));

        handler.handle(adjusted("reservation-event-2", "adjustment-second", "SMALLER_ROSTER", Money.myr(30),
                Money.myr(80), SMALLER_ROSTER_ACCEPTED_AT));

        assertThat(gateway.refunds()).containsExactly(
                new PaymentGateway.RefundRequest(SMALLER_ROSTER_REFUND_KEY, ONLINE_BANKING.value(), Money.myr(90)),
                new PaymentGateway.RefundRequest("adjustment:adjustment-second:refund", ONLINE_BANKING.value(),
                        Money.myr(30)));
        assertThat(hold.held()).isEqualTo(Money.myr(80));
        assertThat(payoutOfTheReservation().gross()).isEqualTo(Money.myr(80));
        assertThat(publisher.published()).containsExactly(
                new RefundPaidIE("event-1", RESERVATION_ID, AISYAH_CUSTOMER_ID, Money.myr(90), "SMALLER_ROSTER",
                        SMALLER_ROSTER_ACCEPTED_AT),
                new RefundPaidIE("event-2", RESERVATION_ID, AISYAH_CUSTOMER_ID, Money.myr(30), "SMALLER_ROSTER",
                        SMALLER_ROSTER_ACCEPTED_AT));
        assertThat(ledger.balanceOf(LedgerAccount.CUSTOMER_HELD)).isEqualTo(Money.myr(80));
    }

    @Test
    @Tag("ROS-8")
    @Tag("PAY-7")
    @DisplayName("A redelivered adjustment refunds nothing a second time: one refund, one event, one saved change.")
    void redeliveredEventChangesNothing() {
        PaymentHold hold = givenAisyahPaidAndReserved();
        handler.handle(smallerRosterAccepted("reservation-event-1"));

        handler.handle(smallerRosterAccepted("reservation-event-1"));

        assertThat(gateway.refunds()).hasSize(1);
        assertThat(publisher.published()).hasSize(1);
        assertThat(holds.saves()).hasSize(1);
        assertThat(payouts.saves()).hasSize(1);
        assertThat(ledger.entries()).hasSize(2);
        assertThat(hold.held()).isEqualTo(Money.myr(110));
        assertThat(payoutOfTheReservation().gross()).isEqualTo(Money.myr(110));
    }

    @Test
    @Tag("PAY-7")
    @DisplayName("An event this handler already handled is not applied again, whatever state the payment is in.")
    void alreadyHandledEventIsNotApplied() {
        PaymentHold hold = givenAisyahPaidAndReserved();
        processed.recordFirstHandling("reservation-event-1", HandleSettlementAdjustedIE.HANDLER);

        handler.handle(smallerRosterAccepted("reservation-event-1"));

        assertThat(hold.held()).isEqualTo(Money.myr(200));
        assertThat(payoutOfTheReservation().gross()).isEqualTo(Money.myr(200));
        assertNothingWasSaidOrSent();
    }

    @Test
    @Tag("ROS-8")
    @Tag("PAY-7")
    @DisplayName("A refund sent again after a failure carries the same idempotency key, which names the adjustment, so "
            + "the gateway pays it once.")
    void moneyKeyDependsOnlyOnTheAdjustment() {
        givenAisyahPaidAndReserved();
        handler.handle(smallerRosterAccepted("reservation-event-1"));

        InMemoryPaymentHoldRepository retriedHolds = new InMemoryPaymentHoldRepository();
        retriedHolds.given(aisyahsRequestedHold());
        InMemoryPayoutRepository retriedPayouts = new InMemoryPayoutRepository();
        retriedPayouts.given(kemasHomesPayoutAtReserved());
        RecordingPaymentGateway retriedGateway = new RecordingPaymentGateway();
        new HandleSettlementAdjustedIE(retriedHolds, retriedPayouts, retriedGateway, new InMemoryLedgerRepository(),
                new RecordingPaymentIEPublisher(), new InMemoryProcessedMessageStore(), time, new SequentialEventIds())
                .handle(smallerRosterAccepted("reservation-event-2"));

        assertThat(retriedGateway.refunds()).extracting(PaymentGateway.RefundRequest::idempotencyKey)
                .containsExactly(SMALLER_ROSTER_REFUND_KEY);
        assertThat(gateway.refunds()).extracting(PaymentGateway.RefundRequest::idempotencyKey)
                .containsExactly(SMALLER_ROSTER_REFUND_KEY);
    }

    @Test
    @Tag("PAY-7")
    @DisplayName("The same adjustment published again under a new event id is refused before any money moves: the "
            + "payout no longer matches the new total.")
    void adjustmentReplayedUnderANewEventIdMovesNoMoney() {
        givenAisyahPaidAndReserved();
        handler.handle(smallerRosterAccepted("reservation-event-1"));

        assertThatThrownBy(() -> handler.handle(smallerRosterAccepted("reservation-event-2")))
                .isInstanceOf(AdjustedTotalMismatchException.class)
                .hasMessageContaining(SMALLER_ROSTER_ADJUSTMENT_ID)
                .hasMessageContaining(RESERVATION_ID);

        assertThat(gateway.refunds()).hasSize(1);
        assertThat(publisher.published()).hasSize(1);
        assertThat(holds.saves()).hasSize(1);
        assertThat(payouts.saves()).hasSize(1);
        assertThat(ledger.entries()).hasSize(2);
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("Reservation says the reservation now costs RM100, but RM90 off Kemas Home's RM200 payout leaves "
            + "RM110: Payment's payout and Reservation's price disagree, so nothing is sent.")
    void aNewTotalThePayoutDoesNotReachIsRefused() {
        givenAisyahPaidAndReserved();

        assertThatThrownBy(() -> handler.handle(adjusted("reservation-event-1", SMALLER_ROSTER_ADJUSTMENT_ID,
                "SMALLER_ROSTER", Money.myr(90), Money.myr(100), SMALLER_ROSTER_ACCEPTED_AT)))
                .isInstanceOf(AdjustedTotalMismatchException.class)
                .hasMessageContaining("MYR 100")
                .hasMessageContaining("MYR 110");

        assertNothingWasSaidOrSent();
    }

    @Test
    @Tag("ROS-8")
    @DisplayName("The payout can't go below zero: a RM90 refund against a RM45 fee payout leaves nothing, which is "
            + "what Reservation says the reservation now costs.")
    void thePayoutNeverGoesBelowZero() {
        givenAisyahPaidAndReserved();
        payoutOfTheReservation().payFeeInstead(Money.myr(45));

        handler.handle(adjusted("reservation-event-1", SMALLER_ROSTER_ADJUSTMENT_ID, "SMALLER_ROSTER", Money.myr(90),
                Money.myr(0), SMALLER_ROSTER_ACCEPTED_AT));

        assertThat(payoutOfTheReservation().gross()).isEqualTo(Money.myr(0));
        assertThat(gateway.refunds()).extracting(PaymentGateway.RefundRequest::amount).containsExactly(Money.myr(90));
    }

    @Test
    @Tag("PAY-7")
    @DisplayName("An adjustment for a reservation whose payment Payment never linked is refused, so it is retried and "
            + "reported, and nothing is skipped.")
    void adjustmentForAnUnknownReservationIsRefused() {
        payouts.given(kemasHomesPayoutAtReserved());

        assertThatThrownBy(() -> handler.handle(smallerRosterAccepted("reservation-event-1")))
                .isInstanceOf(ReservationHoldNotFoundException.class)
                .hasMessageContaining(RESERVATION_ID);

        assertThat(payoutOfTheReservation().gross()).isEqualTo(Money.myr(200));
        assertNothingWasSaidOrSent();
    }

    @Test
    @Tag("PAY-7")
    @DisplayName("An adjustment for a reservation that never had a payout opened is refused, and nothing is sent.")
    void adjustmentWithoutAPayoutIsRefused() {
        PaymentHold hold = aisyahsRequestedHold();
        holds.given(hold);

        assertThatThrownBy(() -> handler.handle(smallerRosterAccepted("reservation-event-1")))
                .isInstanceOf(PayoutNotFoundException.class)
                .hasMessageContaining(RESERVATION_ID);

        assertThat(hold.held()).isEqualTo(Money.myr(200));
        assertThat(gateway.refunds()).isEmpty();
        assertThat(publisher.published()).isEmpty();
        assertThat(holds.saves()).isEmpty();
    }

    @Test
    @Tag("PAY-4")
    @Tag("PAY-7")
    @DisplayName("A refund larger than what is held is refused, never trimmed, and nothing is sent.")
    void refundLargerThanWhatIsHeldIsRefused() {
        givenAisyahPaidAndReserved();

        assertThatThrownBy(() -> handler.handle(adjusted("reservation-event-1", SMALLER_ROSTER_ADJUSTMENT_ID,
                "SMALLER_ROSTER", Money.myr(201), Money.myr(0), SMALLER_ROSTER_ACCEPTED_AT)))
                .isInstanceOf(RefundExceedsHeldAmountException.class);

        assertNothingWasSaidOrSent();
    }

    @Test
    @Tag("PAY-7")
    @DisplayName("A payout that is no longer waiting can't be lowered: the adjustment is refused before any money moves.")
    void adjustmentForAPayoutThatIsNoLongerPendingIsRefused() {
        givenAisyahPaidAndReserved();
        payoutOfTheReservation().makeVoid();

        assertThatThrownBy(() -> handler.handle(smallerRosterAccepted("reservation-event-1")))
                .isInstanceOf(PayoutNotPendingException.class);

        assertNothingWasSaidOrSent();
    }

    @Test
    @Tag("PAY-7")
    @DisplayName("A reason Payment has no refund reason for is refused, and nothing is sent.")
    void adjustmentWithAnUnknownReasonIsRefused() {
        givenAisyahPaidAndReserved();

        assertThatThrownBy(() -> handler.handle(adjusted("reservation-event-1", "adjustment-odd", "ANY_REASON",
                Money.myr(90), Money.myr(110), SMALLER_ROSTER_ACCEPTED_AT)))
                .isInstanceOf(UnknownAdjustmentReasonException.class)
                .hasMessageContaining("ANY_REASON");

        assertNothingWasSaidOrSent();
    }

    @Test
    @Tag("PAY-7")
    @DisplayName("A refund of nothing is not a payable line: it is refused, not skipped, and nothing is sent.")
    void aRefundOfNothingIsRefused() {
        givenAisyahPaidAndReserved();

        assertThatThrownBy(() -> handler.handle(adjusted("reservation-event-1", SMALLER_ROSTER_ADJUSTMENT_ID,
                "SMALLER_ROSTER", Money.myr(0), Money.myr(200), SMALLER_ROSTER_ACCEPTED_AT)))
                .isInstanceOf(AmountNotPayableException.class);

        assertNothingWasSaidOrSent();
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("Nurul leaves at 12:05: the refund is 1.75 × 22.50 = RM39.375, rounded to RM39.38, so the booking totals "
            + "RM160.62: Payment sends the rounded line it is given, to the sen.")
    void theRoundedLineIsSentToTheSen() {
        PaymentHold hold = givenAisyahPaidAndReserved();

        handler.handle(adjusted("reservation-event-1", "adjustment-rounded", "SMALLER_ROSTER", Money.myr("39.38"),
                Money.myr("160.62"), SMALLER_ROSTER_ACCEPTED_AT));

        assertThat(gateway.refunds()).extracting(PaymentGateway.RefundRequest::amount)
                .containsExactly(Money.myr("39.38"));
        assertThat(hold.held()).isEqualTo(Money.myr("160.62"));
        assertThat(payoutOfTheReservation().gross()).isEqualTo(Money.myr("160.62"));
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("A refund line of RM39.375 isn't rounded to the sen: Payment never rounds a line a second time, so it "
            + "is refused and nothing is sent.")
    void anUnroundedLineIsRefused() {
        givenAisyahPaidAndReserved();

        assertThatThrownBy(() -> handler.handle(adjusted("reservation-event-1", "adjustment-rounded", "SMALLER_ROSTER",
                Money.myr("39.375"), Money.myr("160.625"), SMALLER_ROSTER_ACCEPTED_AT)))
                .isInstanceOf(AmountNotPayableException.class);

        assertNothingWasSaidOrSent();
    }
}
