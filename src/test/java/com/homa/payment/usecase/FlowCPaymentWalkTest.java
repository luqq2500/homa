package com.homa.payment.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.CHECKOUT_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.COMPLETED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.KEMAS_HOME;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.NO_SHOW_CONFIRMED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PAID_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PROTECTION_WINDOW_END;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVATION;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVATION_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.checkoutStarted;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.completed;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.kemasHomesVerifiedAccount;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.providerNoShow;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.requested;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.reserved;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.PayoutReleasedIE;
import com.homa.ie.events.RefundPaidIE;
import com.homa.kernel.Money;
import com.homa.payment.domain.Fine;
import com.homa.payment.domain.LedgerAccount;
import com.homa.payment.domain.Payout;
import com.homa.payment.domain.PayoutId;
import com.homa.payment.domain.PayoutStatus;
import com.homa.payment.domain.PaymentAttemptId;
import com.homa.payment.domain.ReservationId;
import com.homa.payment.usecase.dto.PayForReservationRequest;
import com.homa.payment.usecase.dto.ReleaseDuePayoutsRequest;
import com.homa.payment.usecase.ie.HandleReservationCancelledIE;
import com.homa.payment.usecase.ie.HandleReservationCheckoutStartedIE;
import com.homa.payment.usecase.ie.HandleReservationCompletedIE;
import com.homa.payment.usecase.ie.HandleReservationRequestedIE;
import com.homa.payment.usecase.ie.HandleReservationReservedIE;
import com.homa.payment.usecase.port.out.PaymentGateway;
import com.homa.payment.usecase.port.out.PayoutGateway;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Flow C (plan, "Key end-to-end flows"), step 4, as Payment walks it with every port faked. */
class FlowCPaymentWalkTest {

    /** Kemas Home's next two RM300 jobs, paid out on the Sundays after. */
    private static final Instant NEXT_PAYOUT_DUE = at(2026, 12, 13, 14, 0);
    private static final Instant PAYOUT_AFTER_DUE = at(2026, 12, 20, 14, 0);

    private final InMemoryPaymentHoldRepository holds = new InMemoryPaymentHoldRepository();
    private final InMemoryPayoutRepository payouts = new InMemoryPayoutRepository();
    private final InMemoryPayoutAccountRepository accounts = new InMemoryPayoutAccountRepository();
    private final InMemoryFineAccountRepository fineAccounts = new InMemoryFineAccountRepository();
    private final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();
    private final RecordingPaymentGateway paymentGateway = new RecordingPaymentGateway();
    private final RecordingPayoutGateway payoutGateway = new RecordingPayoutGateway();
    private final RecordingPaymentIEPublisher publisher = new RecordingPaymentIEPublisher();
    private final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    private final FixedTimeSource time = new FixedTimeSource(PAID_AT);
    private final SequentialEventIds eventIds = new SequentialEventIds();
    private String paymentReference;

    /** Aisyah pays RM200 on Monday, the reservation is Requested and then Reserved (flow A, steps 3 to 5). */
    private void givenAisyahsReservationIsReserved() {
        accounts.given(kemasHomesVerifiedAccount());
        new HandleReservationCheckoutStartedIE(holds, processed).handle(checkoutStarted("reservation-1"));
        paymentReference = new CustomerPayForReservation(holds, paymentGateway)
                .pay(new PayForReservationRequest(AISYAH_CUSTOMER_ID, CHECKOUT_ID)).paymentReference();
        new ConfirmGatewayPayment(holds, paymentGateway, publisher, processed, time, eventIds)
                .confirm(PaymentUsecaseFixtures.confirmed("gw-1", new PaymentAttemptId(paymentReference), PAID_AT));
        new HandleReservationRequestedIE(holds, ledger, processed, time).handle(requested("reservation-2"));
        time.set(RESERVED_AT);
        new HandleReservationReservedIE(payouts, processed).handle(reserved("reservation-3"));
        publisher.clear();
    }

    private void givenKemasHomeHasTwoMoreRm300PayoutsComingDue() {
        payouts.given(completedRm300Payout("next", NEXT_PAYOUT_DUE));
        payouts.given(completedRm300Payout("after", PAYOUT_AFTER_DUE));
    }

    private static Payout completedRm300Payout(String name, Instant windowEnd) {
        Payout payout = Payout.openForReservation(new PayoutId("payout-" + name),
                new ReservationId("reservation-" + name), KEMAS_HOME, Money.myr(300), HomaFixtures.REQUESTED_AT,
                windowEnd);
        payout.markCompleted(windowEnd.minusSeconds(24 * 3600));
        return payout;
    }

    private void supportConfirmsTheProviderNoShow() {
        time.set(NO_SHOW_CONFIRMED_AT);
        new HandleReservationCancelledIE(holds, payouts, fineAccounts, paymentGateway, ledger, publisher, processed,
                time, eventIds).handle(providerNoShow("reservation-4"));
    }

    private void releaseDueAt(Instant now) {
        time.set(now);
        new ReleaseDuePayouts(payouts, accounts, fineAccounts, payoutGateway, ledger, publisher, time, eventIds)
                .releaseDue(new ReleaseDuePayoutsRequest());
    }

    @Test
    @Tag("PAY-4")
    @Tag("PAY-7")
    @DisplayName("After a provider no-show, Aisyah gets RM200 back plus RM20 from Homa. Kemas Home's RM105 fine repays "
            + "the RM20, and Homa keeps RM85.")
    void providerNoShowRefundsCompensatesAndFines() {
        givenAisyahsReservationIsReserved();

        supportConfirmsTheProviderNoShow();

        assertThat(paymentGateway.refunds()).containsExactly(new PaymentGateway.RefundRequest(
                "settlement:settlement-no-show:customerRefund", paymentReference, Money.myr(200)));
        assertThat(paymentGateway.compensations()).containsExactly(new PaymentGateway.CompensationRequest(
                "settlement:settlement-no-show:compensation", paymentReference, Money.myr(20)));
        assertThat(publisher.published()).containsExactly(
                new RefundPaidIE("event-2", RESERVATION_ID, AISYAH_CUSTOMER_ID, Money.myr(200), "CANCELLED",
                        NO_SHOW_CONFIRMED_AT),
                new RefundPaidIE("event-3", RESERVATION_ID, AISYAH_CUSTOMER_ID, Money.myr(20), "COMPENSATION",
                        NO_SHOW_CONFIRMED_AT));
        assertThat(fineAccounts.findByProviderId(KEMAS_HOME).orElseThrow().fines()).containsExactly(
                new Fine(RESERVATION, Money.myr(105), Money.myr(105), NO_SHOW_CONFIRMED_AT));
        assertThat(payouts.findByReservationId(RESERVATION).orElseThrow().status()).isEqualTo(PayoutStatus.VOID);
        assertThat(ledger.balanceOf(LedgerAccount.CUSTOMER_HELD)).isEqualTo(Money.myr(0));
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_COMPENSATION_EXPENSE)).isEqualTo(Money.myr(20));
        assertThat(ledger.balanceOf(LedgerAccount.FINE_RECEIVABLE)).isEqualTo(Money.myr(105));
        assertThat(ledger.homaKeeps()).isEqualTo(Money.myr(85));
    }

    @Test
    @Tag("PAY-5")
    @DisplayName("Kemas Home's RM105 fine is recovered from its future payouts, at most 20% after the platform fee each "
            + "time: RM50.70 from each of two RM300 payouts, and nothing is released for the no-show reservation.")
    void fineIsRecoveredFromFuturePayoutsAtMostTwentyPercentEachTime() {
        givenAisyahsReservationIsReserved();
        givenKemasHomeHasTwoMoreRm300PayoutsComingDue();
        supportConfirmsTheProviderNoShow();
        publisher.clear();

        releaseDueAt(PROTECTION_WINDOW_END);
        assertThat(payoutGateway.transfers()).isEmpty();

        releaseDueAt(NEXT_PAYOUT_DUE);
        releaseDueAt(PAYOUT_AFTER_DUE);

        assertThat(payoutGateway.transfers()).extracting(PayoutGateway.Transfer::amount)
                .containsExactly(Money.myr("202.80"), Money.myr("202.80"));
        assertThat(publisher.published()).extracting(event -> ((PayoutReleasedIE) event).fineDeduction())
                .containsExactly(Money.myr("50.70"), Money.myr("50.70"));
        assertThat(fineAccounts.findByProviderId(KEMAS_HOME).orElseThrow().outstanding())
                .isEqualTo(Money.myr("3.60"));
        assertThat(ledger.balanceOf(LedgerAccount.FINE_RECEIVABLE)).isEqualTo(Money.myr("3.60"));
        assertThat(ledger.balanceOf(LedgerAccount.PROVIDER_PAYABLE)).isEqualTo(Money.myr(0));
        assertThat(ledger.homaKeeps()).isEqualTo(Money.myr(85).plus(Money.myr("93")));
    }

    @Test
    @Tag("NSH-6")
    @Tag("PAY-4")
    @DisplayName("Kemas Home never came but marked the job Completed with an old photo. Aisyah reports on Sunday. "
            + "Support confirms: the job payout is voided, so nothing is released at Sunday 14:00, and Aisyah is "
            + "refunded.")
    void providerNoShowConfirmedAfterCompletedVoidsTheJobPayoutBeforeItIsReleased() {
        givenAisyahsReservationIsReserved();
        time.set(COMPLETED_AT);
        new HandleReservationCompletedIE(payouts, processed).handle(completed("reservation-4"));
        time.set(at(2026, 12, 6, 10, 0));

        new HandleReservationCancelledIE(holds, payouts, fineAccounts, paymentGateway, ledger, publisher, processed,
                time, eventIds).handle(providerNoShow("reservation-5"));
        releaseDueAt(PROTECTION_WINDOW_END);

        assertThat(payouts.findByReservationId(RESERVATION).orElseThrow().status()).isEqualTo(PayoutStatus.VOID);
        assertThat(payoutGateway.transfers()).isEmpty();
        assertThat(paymentGateway.refunds()).extracting(PaymentGateway.RefundRequest::amount)
                .containsExactly(Money.myr(200));
        assertThat(ledger.balanceOf(LedgerAccount.PROVIDER_PAYABLE)).isEqualTo(Money.myr(0));
        assertThat(ledger.homaKeeps()).isEqualTo(Money.myr(85));
    }
}
