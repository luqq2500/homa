package com.homa.payment.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_NAME;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ACCOUNT_NUMBER;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.BANK;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.CHECKOUT_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.KEMAS_HOME;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.KEMAS_HOMES_ACCOUNT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.LATE_CANCELLATION_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PAID_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PROTECTION_WINDOW_END;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVATION;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVATION_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.checkoutStarted;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.kemasHomesVerifiedAccount;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.lateCustomerCancellation;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.requested;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.reserved;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.PaymentHeldIE;
import com.homa.ie.events.PayoutReleasedIE;
import com.homa.ie.events.RefundPaidIE;
import com.homa.kernel.Money;
import com.homa.payment.domain.LedgerAccount;
import com.homa.payment.domain.PayoutKind;
import com.homa.payment.domain.PayoutStatus;
import com.homa.payment.domain.PaymentAttemptId;
import com.homa.payment.usecase.dto.PayForReservationRequest;
import com.homa.payment.usecase.dto.ReleaseDuePayoutsRequest;
import com.homa.payment.usecase.ie.HandleReservationCancelledIE;
import com.homa.payment.usecase.ie.HandleReservationCheckoutStartedIE;
import com.homa.payment.usecase.ie.HandleReservationRequestedIE;
import com.homa.payment.usecase.ie.HandleReservationReservedIE;
import com.homa.payment.usecase.port.out.PaymentGateway;
import com.homa.payment.usecase.port.out.PayoutGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Flow B (plan, "Key end-to-end flows"), step 3, as Payment walks it with every port faked. */
class FlowBPaymentWalkTest {

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

    private ReleaseDuePayouts releaser() {
        return new ReleaseDuePayouts(payouts, accounts, fineAccounts, payoutGateway, ledger, publisher, time,
                eventIds);
    }

    @Test
    @Tag("CCX-6")
    @Tag("PAY-3")
    @Tag("PAY-6")
    @Tag("PAY-7")
    @Tag("PAY-8")
    @DisplayName("Aisyah cancels on Friday 22:00, after the Standard cut-off: RM155 is refunded at once, Kemas Home's "
            + "RM45 fee waits until Sunday 14:00, and then the platform fee RM6.98 is taken and RM38.02 is sent.")
    void lateCancellationRefundsAtOnceAndPaysTheFeeAfterTheProtectionWindow() {
        accounts.given(kemasHomesVerifiedAccount());
        new HandleReservationCheckoutStartedIE(holds, processed).handle(checkoutStarted("reservation-1"));
        String reference = new CustomerPayForReservation(holds, paymentGateway)
                .pay(new PayForReservationRequest(AISYAH_CUSTOMER_ID, CHECKOUT_ID)).paymentReference();
        new ConfirmGatewayPayment(holds, paymentGateway, publisher, processed, time, eventIds)
                .confirm(PaymentUsecaseFixtures.confirmed("gw-1", new PaymentAttemptId(reference), PAID_AT));
        new HandleReservationRequestedIE(holds, ledger, processed, time).handle(requested("reservation-2"));
        time.set(RESERVED_AT);
        new HandleReservationReservedIE(payouts, processed).handle(reserved("reservation-3"));

        time.set(LATE_CANCELLATION_AT);
        new HandleReservationCancelledIE(holds, payouts, fineAccounts, paymentGateway, ledger, publisher, processed,
                time, eventIds).handle(lateCustomerCancellation("reservation-4"));

        assertThat(paymentGateway.refunds()).containsExactly(new PaymentGateway.RefundRequest(
                "settlement:settlement-late-cancellation:customerRefund", reference, Money.myr(155)));
        assertThat(payouts.findByReservationId(RESERVATION).orElseThrow().kind()).isEqualTo(PayoutKind.FEE);
        assertThat(releaser().releaseDue(new ReleaseDuePayoutsRequest()).releasedPayoutIds()).isEmpty();

        time.set(PROTECTION_WINDOW_END.minusSeconds(60));
        assertThat(releaser().releaseDue(new ReleaseDuePayoutsRequest()).releasedPayoutIds()).isEmpty();

        time.set(PROTECTION_WINDOW_END);
        releaser().releaseDue(new ReleaseDuePayoutsRequest());

        String payoutId = payouts.findByReservationId(RESERVATION).orElseThrow().id().value();
        assertThat(payouts.findByReservationId(RESERVATION).orElseThrow().status()).isEqualTo(PayoutStatus.RELEASED);
        assertThat(publisher.published()).containsExactly(
                new PaymentHeldIE("event-1", CHECKOUT_ID, Money.myr(200), PAID_AT, PAID_AT),
                new RefundPaidIE("event-2", RESERVATION_ID, AISYAH_CUSTOMER_ID, Money.myr(155), "CANCELLED",
                        LATE_CANCELLATION_AT),
                new PayoutReleasedIE("event-3", payoutId, RESERVATION_ID, KEMAS_HOME_PROVIDER_ID, "FEE",
                        Money.myr(45), Money.myr("6.98"), Money.myr(0), Money.myr("38.02"), PROTECTION_WINDOW_END));
        assertThat(payoutGateway.transfers()).containsExactly(new PayoutGateway.Transfer("payout:" + payoutId,
                KEMAS_HOMES_ACCOUNT.value(), BANK, ACCOUNT_NUMBER, KEMAS_HOME_NAME, Money.myr("38.02")));
        assertThat(fineAccounts.findByProviderId(KEMAS_HOME)).isEmpty();
    }

    @Test
    @Tag("CCX-6")
    @Tag("PAY-4")
    @Tag("PAY-6")
    @DisplayName("Aisyah pays RM45 and gets RM155 back: once Kemas Home is paid, Homa holds nothing for her and keeps "
            + "the RM6.98 platform fee.")
    void ledgerShowsWhereEveryRinggitWent() {
        accounts.given(kemasHomesVerifiedAccount());
        new HandleReservationCheckoutStartedIE(holds, processed).handle(checkoutStarted("reservation-1"));
        String reference = new CustomerPayForReservation(holds, paymentGateway)
                .pay(new PayForReservationRequest(AISYAH_CUSTOMER_ID, CHECKOUT_ID)).paymentReference();
        new ConfirmGatewayPayment(holds, paymentGateway, publisher, processed, time, eventIds)
                .confirm(PaymentUsecaseFixtures.confirmed("gw-1", new PaymentAttemptId(reference), PAID_AT));
        new HandleReservationRequestedIE(holds, ledger, processed, time).handle(requested("reservation-2"));
        new HandleReservationReservedIE(payouts, processed).handle(reserved("reservation-3"));
        time.set(LATE_CANCELLATION_AT);
        new HandleReservationCancelledIE(holds, payouts, fineAccounts, paymentGateway, ledger, publisher, processed,
                time, eventIds).handle(lateCustomerCancellation("reservation-4"));

        assertThat(ledger.balanceOf(LedgerAccount.CUSTOMER_HELD)).isEqualTo(Money.myr(45));

        time.set(PROTECTION_WINDOW_END);
        releaser().releaseDue(new ReleaseDuePayoutsRequest());

        assertThat(ledger.balanceOf(LedgerAccount.CUSTOMER_HELD)).isEqualTo(Money.myr(0));
        assertThat(ledger.balanceOf(LedgerAccount.PROVIDER_PAYABLE)).isEqualTo(Money.myr(0));
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_REVENUE)).isEqualTo(Money.myr("6.98"));
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_BANK)).isEqualTo(Money.myr("6.98"));
        assertThat(ledger.homaKeeps()).isEqualTo(Money.myr("6.98"));
    }
}
