package com.homa.payment.usecase.ie;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.KEMAS_HOME;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.LATE_CANCELLATION_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.NO_SHOW_CONFIRMED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ONLINE_BANKING;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PAID_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PROTECTION_WINDOW_END;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVATION;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVATION_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.aisyahsRequestedHold;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.cancelled;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.kemasHomesCompletedPayout;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.kemasHomesPayoutAtReserved;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.lateCustomerCancellation;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.providerNoShow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.RefundPaidIE;
import com.homa.ie.events.ReservationCancelledIE;
import com.homa.kernel.Money;
import com.homa.payment.domain.Fine;
import com.homa.payment.domain.LedgerAccount;
import com.homa.payment.domain.LedgerEntry;
import com.homa.payment.domain.LedgerMovement;
import com.homa.payment.domain.PaymentHold;
import com.homa.payment.domain.PaymentHoldStatus;
import com.homa.payment.domain.Payout;
import com.homa.payment.domain.PayoutKind;
import com.homa.payment.domain.PayoutStatus;
import com.homa.payment.domain.ProviderFineAccount;
import com.homa.payment.domain.ReservationId;
import com.homa.payment.domain.exception.PayoutNotAwaitingCompletionException;
import com.homa.payment.domain.exception.PayoutNotPendingException;
import com.homa.payment.domain.exception.RefundExceedsHeldAmountException;
import com.homa.payment.usecase.FixedTimeSource;
import com.homa.payment.usecase.InMemoryFineAccountRepository;
import com.homa.payment.usecase.InMemoryLedgerRepository;
import com.homa.payment.usecase.InMemoryPaymentHoldRepository;
import com.homa.payment.usecase.InMemoryPayoutRepository;
import com.homa.payment.usecase.InMemoryProcessedMessageStore;
import com.homa.payment.usecase.RecordingPaymentGateway;
import com.homa.payment.usecase.RecordingPaymentIEPublisher;
import com.homa.payment.usecase.SequentialEventIds;
import com.homa.payment.usecase.exception.PayoutNotFoundException;
import com.homa.payment.usecase.exception.ReservationHoldNotFoundException;
import com.homa.payment.usecase.port.out.PaymentGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleReservationCancelledIETest {

    private static final Money NOTHING = Money.myr(0);
    private static final String LATE_REFUND_KEY = "settlement:settlement-late-cancellation:customerRefund";
    private static final String NO_SHOW_REFUND_KEY = "settlement:settlement-no-show:customerRefund";
    private static final String NO_SHOW_COMPENSATION_KEY = "settlement:settlement-no-show:compensation";

    private final InMemoryPaymentHoldRepository holds = new InMemoryPaymentHoldRepository();
    private final InMemoryPayoutRepository payouts = new InMemoryPayoutRepository();
    private final InMemoryFineAccountRepository fineAccounts = new InMemoryFineAccountRepository();
    private final RecordingPaymentGateway gateway = new RecordingPaymentGateway();
    private final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();
    private final RecordingPaymentIEPublisher publisher = new RecordingPaymentIEPublisher();
    private final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    private final FixedTimeSource time = new FixedTimeSource(LATE_CANCELLATION_AT);
    private final HandleReservationCancelledIE handler = new HandleReservationCancelledIE(holds, payouts,
            fineAccounts, gateway, ledger, publisher, processed, time, new SequentialEventIds());

    /** Aisyah's RM200 is held and Kemas Home's payout is open, as they are once the reservation is Reserved. */
    private PaymentHold givenAisyahPaidAndReserved() {
        PaymentHold hold = aisyahsRequestedHold();
        holds.given(hold);
        payouts.given(kemasHomesPayoutAtReserved());
        ledger.post(new LedgerEntry(LedgerMovement.PAYMENT_HELD, Money.myr(200), PAID_AT));
        return hold;
    }

    /** Support confirms the Provider No Show at 10:30 on Saturday, and Reservation publishes the settlement. */
    private void handleNoShow(String eventId) {
        time.set(NO_SHOW_CONFIRMED_AT);
        handler.handle(providerNoShow(eventId));
    }

    private Payout payoutOfTheReservation() {
        return payouts.findByReservationId(RESERVATION).orElseThrow();
    }

    @Test
    @Tag("CCX-6")
    @Tag("PAY-6")
    @Tag("PAY-7")
    @DisplayName("Aisyah cancels on Friday 22:00: RM155 goes back at once, and Kemas Home's payout becomes a RM45 fee "
            + "payout held until Sunday 14:00.")
    void lateCustomerCancellationRefundsNowAndHoldsTheFeeUntilTheWindowEnds() {
        PaymentHold hold = givenAisyahPaidAndReserved();

        handler.handle(lateCustomerCancellation("reservation-event-1"));

        assertThat(gateway.refunds()).containsExactly(
                new PaymentGateway.RefundRequest(LATE_REFUND_KEY, ONLINE_BANKING.value(), Money.myr(155)));
        assertThat(gateway.compensations()).isEmpty();
        assertThat(hold.held()).isEqualTo(Money.myr(45));
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.HELD);
        assertThat(holds.saves()).containsExactly(hold);
        Payout payout = payoutOfTheReservation();
        assertThat(payout.kind()).isEqualTo(PayoutKind.FEE);
        assertThat(payout.gross()).isEqualTo(Money.myr(45));
        assertThat(payout.status()).isEqualTo(PayoutStatus.PENDING);
        assertThat(payout.protectionWindowEnd()).isEqualTo(PROTECTION_WINDOW_END);
        assertThat(payout.releasableAt(LATE_CANCELLATION_AT)).isFalse();
        assertThat(payout.releasableAt(PROTECTION_WINDOW_END)).isTrue();
        assertThat(payouts.saves()).containsExactly(payout);
        assertThat(fineAccounts.saves()).isEmpty();
        assertThat(processed.recorded()).containsExactly("reservation-event-1|payment.ReservationCancelled");
    }

    @Test
    @Tag("CCX-6")
    @Tag("PAY-7")
    @DisplayName("Aisyah's RM155 refund is sent at Friday 22:00: the event says so, field by field.")
    void refundPaidIsPublishedForTheRefund() {
        givenAisyahPaidAndReserved();

        handler.handle(lateCustomerCancellation("reservation-event-1"));

        assertThat(publisher.published()).containsExactly(new RefundPaidIE("event-1", RESERVATION_ID,
                AISYAH_CUSTOMER_ID, Money.myr(155), "CANCELLED", LATE_CANCELLATION_AT));
        assertThat(ledger.entries()).last()
                .isEqualTo(new LedgerEntry(LedgerMovement.REFUND_SENT, Money.myr(155), LATE_CANCELLATION_AT));
        assertThat(ledger.balanceOf(LedgerAccount.CUSTOMER_HELD)).isEqualTo(Money.myr(45));
    }

    @Test
    @Tag("PAY-4")
    @Tag("PAY-7")
    @DisplayName("After a provider no-show, Aisyah gets RM200 back plus RM20 from Homa, both at once.")
    void providerNoShowRefundsAndPaysCompensationAtOnce() {
        PaymentHold hold = givenAisyahPaidAndReserved();

        handleNoShow("reservation-event-1");

        assertThat(gateway.refunds()).containsExactly(
                new PaymentGateway.RefundRequest(NO_SHOW_REFUND_KEY, ONLINE_BANKING.value(), Money.myr(200)));
        assertThat(gateway.compensations()).containsExactly(new PaymentGateway.CompensationRequest(
                NO_SHOW_COMPENSATION_KEY, ONLINE_BANKING.value(), Money.myr(20)));
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.REFUNDED);
        assertThat(publisher.published()).containsExactly(
                new RefundPaidIE("event-1", RESERVATION_ID, AISYAH_CUSTOMER_ID, Money.myr(200), "CANCELLED",
                        NO_SHOW_CONFIRMED_AT),
                new RefundPaidIE("event-2", RESERVATION_ID, AISYAH_CUSTOMER_ID, Money.myr(20), "COMPENSATION",
                        NO_SHOW_CONFIRMED_AT));
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("Kemas Home's RM105 fine is added to its fine account, with the reservation it came from; its "
            + "payout is void.")
    void providerNoShowAddsTheFineAndVoidsThePayout() {
        givenAisyahPaidAndReserved();

        handleNoShow("reservation-event-1");

        ProviderFineAccount account = fineAccounts.findByProviderId(KEMAS_HOME).orElseThrow();
        assertThat(account.fines()).containsExactly(
                new Fine(RESERVATION, Money.myr(105), Money.myr(105), NO_SHOW_CONFIRMED_AT));
        assertThat(fineAccounts.saves()).containsExactly(account);
        assertThat(payoutOfTheReservation().status()).isEqualTo(PayoutStatus.VOID);
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("After a provider no-show, Aisyah gets RM200 back plus RM20 from Homa. Kemas Home's RM105 fine repays "
            + "the RM20, and Homa keeps RM85.")
    void homaPaysTheCompensationAndKeepsTheRestOfTheFine() {
        givenAisyahPaidAndReserved();

        handleNoShow("reservation-event-1");

        assertThat(ledger.balanceOf(LedgerAccount.CUSTOMER_HELD)).isEqualTo(NOTHING);
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_COMPENSATION_EXPENSE)).isEqualTo(Money.myr(20));
        assertThat(ledger.balanceOf(LedgerAccount.FINE_RECEIVABLE)).isEqualTo(Money.myr(105));
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_REVENUE)).isEqualTo(Money.myr(105));
        assertThat(ledger.homaKeeps()).isEqualTo(Money.myr(85));
        assertThat(ledger.entries()).extracting(LedgerEntry::movement).containsExactly(LedgerMovement.PAYMENT_HELD,
                LedgerMovement.REFUND_SENT, LedgerMovement.COMPENSATION_PAID, LedgerMovement.FINE_IMPOSED);
        assertThat(ledger.entries()).extracting(LedgerEntry::postedAt)
                .containsExactly(PAID_AT, NO_SHOW_CONFIRMED_AT, NO_SHOW_CONFIRMED_AT, NO_SHOW_CONFIRMED_AT);
    }

    @Test
    @Tag("NSH-6")
    @Tag("PAY-4")
    @DisplayName("Kemas Home never came but marked the job Completed with an old photo. Aisyah reports on Sunday. "
            + "Support confirms: its unreleased job payout is voided first, then Aisyah is refunded and the fine "
            + "is recorded.")
    void providerNoShowAfterCompletedVoidsTheJobPayout() {
        PaymentHold hold = aisyahsRequestedHold();
        holds.given(hold);
        Payout completed = kemasHomesCompletedPayout();
        payouts.given(completed);

        handleNoShow("reservation-event-1");

        assertThat(completed.status()).isEqualTo(PayoutStatus.VOID);
        assertThat(completed.kind()).isEqualTo(PayoutKind.JOB);
        assertThat(payouts.saves()).containsExactly(completed);
        assertThat(gateway.refunds()).extracting(PaymentGateway.RefundRequest::amount).containsExactly(Money.myr(200));
        assertThat(fineAccounts.findByProviderId(KEMAS_HOME).orElseThrow().outstanding()).isEqualTo(Money.myr(105));
    }

    @Test
    @Tag("NSH-6")
    @DisplayName("The payout state decides, not the cause: a job already marked Completed is never turned into a fee "
            + "payout, so a settlement that owes the provider a fee for it is refused and nothing is sent.")
    void completedJobPayoutIsNotTurnedIntoAFeePayout() {
        holds.given(aisyahsRequestedHold());
        Payout completed = kemasHomesCompletedPayout();
        payouts.given(completed);

        assertThatThrownBy(() -> handler.handle(lateCustomerCancellation("reservation-event-1")))
                .isInstanceOf(PayoutNotAwaitingCompletionException.class);

        assertThat(completed.status()).isEqualTo(PayoutStatus.PENDING);
        assertThat(gateway.refunds()).isEmpty();
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("PCX-1")
    @Tag("PAY-4")
    @DisplayName("Kemas Home cancels on Wednesday: fined RM45, Aisyah gets RM200. No compensation is paid.")
    void providerCancellingEarlyIsFinedAndTheCustomerIsRefundedInFull() {
        givenAisyahPaidAndReserved();

        handler.handle(cancelled("reservation-event-1", "PROVIDER_CANCELLED", LATE_CANCELLATION_AT,
                "settlement-early-provider", NOTHING, Money.myr(200), NOTHING, Money.myr(45), NOTHING));

        assertThat(gateway.refunds()).extracting(PaymentGateway.RefundRequest::amount).containsExactly(Money.myr(200));
        assertThat(gateway.compensations()).isEmpty();
        assertThat(publisher.published()).hasSize(1);
        assertThat(fineAccounts.findByProviderId(KEMAS_HOME).orElseThrow().outstanding()).isEqualTo(Money.myr(45));
        assertThat(payoutOfTheReservation().status()).isEqualTo(PayoutStatus.VOID);
        assertThat(ledger.homaKeeps()).isEqualTo(Money.myr(45));
    }

    @Test
    @Tag("PCX-2")
    @Tag("PAY-4")
    @DisplayName("Kemas Home cancels on Friday 20:00: fined RM65, Aisyah gets RM206: RM200 back and RM6 from Homa.")
    void providerCancellingLateIsFinedAndTheCustomerGetsTheRefundPlusCompensation() {
        givenAisyahPaidAndReserved();

        handler.handle(cancelled("reservation-event-1", "PROVIDER_CANCELLED", LATE_CANCELLATION_AT,
                "settlement-late-provider", NOTHING, Money.myr(200), Money.myr(6), Money.myr(65), NOTHING));

        assertThat(gateway.refunds()).extracting(PaymentGateway.RefundRequest::amount).containsExactly(Money.myr(200));
        assertThat(gateway.compensations()).extracting(PaymentGateway.CompensationRequest::amount)
                .containsExactly(Money.myr(6));
        assertThat(ledger.homaKeeps()).isEqualTo(Money.myr(59));
    }

    @Test
    @Tag("CCX-1")
    @Tag("PAY-4")
    @DisplayName("Reserved Monday 22:00, Aisyah cancels at 23:30 for free: she gets RM200 back, nothing is owed to "
            + "Kemas Home, no fine is recorded and no compensation is paid.")
    void freeCancellationRefundsInFullAndVoidsThePayout() {
        PaymentHold hold = givenAisyahPaidAndReserved();

        handler.handle(cancelled("reservation-event-1", "CUSTOMER_CANCELLED", LATE_CANCELLATION_AT,
                "settlement-grace", NOTHING, Money.myr(200), NOTHING, NOTHING, NOTHING));

        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.REFUNDED);
        assertThat(gateway.refunds()).extracting(PaymentGateway.RefundRequest::idempotencyKey)
                .containsExactly("settlement:settlement-grace:customerRefund");
        assertThat(gateway.compensations()).isEmpty();
        assertThat(payoutOfTheReservation().status()).isEqualTo(PayoutStatus.VOID);
        assertThat(fineAccounts.saves()).isEmpty();
        assertThat(ledger.homaKeeps()).isEqualTo(NOTHING);
    }

    @Test
    @Tag("PAY-6")
    @Tag("PAY-7")
    @DisplayName("A fee as large as the whole payment leaves nothing to refund: no refund is sent, and Kemas Home's "
            + "payout becomes a RM200 fee payout.")
    void feeAsLargeAsThePaymentSendsNoRefund() {
        PaymentHold hold = givenAisyahPaidAndReserved();

        handler.handle(cancelled("reservation-event-1", "CUSTOMER_NO_SHOW", LATE_CANCELLATION_AT,
                "settlement-whole-fee", Money.myr(200), NOTHING, NOTHING, NOTHING, Money.myr(200)));

        assertThat(gateway.refunds()).isEmpty();
        assertThat(publisher.published()).isEmpty();
        assertThat(holds.saves()).isEmpty();
        assertThat(hold.held()).isEqualTo(Money.myr(200));
        assertThat(payoutOfTheReservation().kind()).isEqualTo(PayoutKind.FEE);
        assertThat(payoutOfTheReservation().gross()).isEqualTo(Money.myr(200));
        assertThat(ledger.balanceOf(LedgerAccount.CUSTOMER_HELD)).isEqualTo(Money.myr(200));
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("Kemas Home already owes RM45 from an earlier reservation: the new RM105 fine goes after it, so the "
            + "older fine is cleared first.")
    void newFineIsRecordedAfterTheOnesBefore() {
        givenAisyahPaidAndReserved();
        ProviderFineAccount owing = ProviderFineAccount.openFor(KEMAS_HOME);
        ReservationId earlier = new ReservationId("reservation-earlier");
        owing.recordFine(earlier, Money.myr(45), PAID_AT);
        fineAccounts.given(owing);

        handleNoShow("reservation-event-1");

        assertThat(owing.fines()).extracting(Fine::reservationId).containsExactly(earlier, RESERVATION);
        assertThat(owing.outstanding()).isEqualTo(Money.myr(150));
        assertThat(fineAccounts.saves()).containsExactly(owing);
    }

    @Test
    @Tag("PAY-7")
    @DisplayName("A redelivered cancellation refunds, compensates and fines nothing a second time.")
    void redeliveredEventChangesNothing() {
        givenAisyahPaidAndReserved();
        handleNoShow("reservation-event-1");

        handleNoShow("reservation-event-1");

        assertThat(gateway.refunds()).hasSize(1);
        assertThat(gateway.compensations()).hasSize(1);
        assertThat(publisher.published()).hasSize(2);
        assertThat(fineAccounts.findByProviderId(KEMAS_HOME).orElseThrow().fines()).hasSize(1);
        assertThat(holds.saves()).hasSize(1);
        assertThat(payouts.saves()).hasSize(1);
        assertThat(ledger.entries()).hasSize(4);
    }

    @Test
    @Tag("PAY-7")
    @DisplayName("An event this handler already handled is not applied again, whatever state the payment is in.")
    void alreadyHandledEventIsNotApplied() {
        PaymentHold hold = givenAisyahPaidAndReserved();
        processed.recordFirstHandling("reservation-event-1", HandleReservationCancelledIE.HANDLER);

        handleNoShow("reservation-event-1");

        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.HELD);
        assertThat(payoutOfTheReservation().status()).isEqualTo(PayoutStatus.PENDING);
        assertThat(gateway.refunds()).isEmpty();
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("PAY-4")
    @Tag("PAY-7")
    @DisplayName("A refund and a compensation sent again after a failure carry the same idempotency keys, which name "
            + "the settlement and the line, so the gateway pays each once.")
    void moneyKeysDependOnlyOnTheSettlementAndTheLine() {
        givenAisyahPaidAndReserved();
        handleNoShow("reservation-event-1");

        InMemoryPaymentHoldRepository retriedHolds = new InMemoryPaymentHoldRepository();
        retriedHolds.given(aisyahsRequestedHold());
        InMemoryPayoutRepository retriedPayouts = new InMemoryPayoutRepository();
        retriedPayouts.given(kemasHomesPayoutAtReserved());
        RecordingPaymentGateway retriedGateway = new RecordingPaymentGateway();
        new HandleReservationCancelledIE(retriedHolds, retriedPayouts, new InMemoryFineAccountRepository(),
                retriedGateway, new InMemoryLedgerRepository(), new RecordingPaymentIEPublisher(),
                new InMemoryProcessedMessageStore(), time, new SequentialEventIds())
                .handle(providerNoShow("reservation-event-2"));

        assertThat(retriedGateway.refunds()).extracting(PaymentGateway.RefundRequest::idempotencyKey)
                .containsExactly(NO_SHOW_REFUND_KEY);
        assertThat(retriedGateway.compensations()).extracting(PaymentGateway.CompensationRequest::idempotencyKey)
                .containsExactly(NO_SHOW_COMPENSATION_KEY);
        assertThat(gateway.refunds()).extracting(PaymentGateway.RefundRequest::idempotencyKey)
                .containsExactly(NO_SHOW_REFUND_KEY);
        assertThat(gateway.compensations()).extracting(PaymentGateway.CompensationRequest::idempotencyKey)
                .containsExactly(NO_SHOW_COMPENSATION_KEY);
    }

    @Test
    @Tag("PAY-7")
    @DisplayName("The same settlement published again under a new event id is refused before any money moves: the "
            + "payout was settled already.")
    void settlementReplayedUnderANewEventIdMovesNoMoney() {
        givenAisyahPaidAndReserved();
        handleNoShow("reservation-event-1");

        assertThatThrownBy(() -> handleNoShow("reservation-event-2"))
                .isInstanceOf(PayoutNotPendingException.class);

        assertThat(gateway.refunds()).hasSize(1);
        assertThat(gateway.compensations()).hasSize(1);
        assertThat(fineAccounts.findByProviderId(KEMAS_HOME).orElseThrow().fines()).hasSize(1);
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("A cancellation for a reservation whose payment Payment never linked is refused, so it is retried "
            + "and reported.")
    void cancellationForAnUnknownReservationIsRefused() {
        payouts.given(kemasHomesPayoutAtReserved());

        assertThatThrownBy(() -> handleNoShow("reservation-event-1"))
                .isInstanceOf(ReservationHoldNotFoundException.class)
                .hasMessageContaining(RESERVATION_ID);
        assertThat(gateway.refunds()).isEmpty();
        assertThat(payoutOfTheReservation().status()).isEqualTo(PayoutStatus.PENDING);
    }

    @Test
    @Tag("PAY-6")
    @DisplayName("A cancellation for a reservation that never had a payout opened is refused, and nothing is sent.")
    void cancellationWithoutAPayoutIsRefused() {
        holds.given(aisyahsRequestedHold());

        assertThatThrownBy(() -> handleNoShow("reservation-event-1"))
                .isInstanceOf(PayoutNotFoundException.class)
                .hasMessageContaining(RESERVATION_ID);
        assertThat(gateway.refunds()).isEmpty();
        assertThat(gateway.compensations()).isEmpty();
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("A refund line larger than what is held is refused, never trimmed, and nothing is sent.")
    void refundLargerThanWhatIsHeldIsRefused() {
        givenAisyahPaidAndReserved();

        assertThatThrownBy(() -> handler.handle(cancelled("reservation-event-1", "PROVIDER_CANCELLED",
                LATE_CANCELLATION_AT, "settlement-too-much", NOTHING, Money.myr(201), NOTHING,
                NOTHING, NOTHING))).isInstanceOf(RefundExceedsHeldAmountException.class);

        assertThat(gateway.refunds()).isEmpty();
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("Payment executes the lines it is given and never re-derives them: the settlement's cause is not "
            + "read, only its amounts.")
    void linesAreExecutedWhateverTheCauseSays() {
        givenAisyahPaidAndReserved();
        ReservationCancelledIE oddlyNamed = cancelled("reservation-event-1", "ANY_CAUSE", LATE_CANCELLATION_AT,
                "settlement-odd", NOTHING, Money.myr(155), Money.myr(3), Money.myr(10), Money.myr(45));

        handler.handle(oddlyNamed);

        assertThat(gateway.refunds()).extracting(PaymentGateway.RefundRequest::amount).containsExactly(Money.myr(155));
        assertThat(gateway.compensations()).extracting(PaymentGateway.CompensationRequest::amount)
                .containsExactly(Money.myr(3));
        assertThat(payoutOfTheReservation().gross()).isEqualTo(Money.myr(45));
        assertThat(fineAccounts.findByProviderId(KEMAS_HOME).orElseThrow().outstanding()).isEqualTo(Money.myr(10));
    }
}
