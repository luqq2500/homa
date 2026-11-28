package com.homa.payment.usecase.ie;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ONLINE_BANKING;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PAID_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVATION_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.WITHDRAWN_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.aisyahsRequestedHold;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.withdrawn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.RefundPaidIE;
import com.homa.kernel.Money;
import com.homa.payment.domain.LedgerAccount;
import com.homa.payment.domain.LedgerEntry;
import com.homa.payment.domain.LedgerMovement;
import com.homa.payment.domain.PaymentHold;
import com.homa.payment.domain.PaymentHoldStatus;
import com.homa.payment.usecase.FixedTimeSource;
import com.homa.payment.usecase.InMemoryLedgerRepository;
import com.homa.payment.usecase.InMemoryPaymentHoldRepository;
import com.homa.payment.usecase.InMemoryProcessedMessageStore;
import com.homa.payment.usecase.RecordingPaymentGateway;
import com.homa.payment.usecase.RecordingPaymentIEPublisher;
import com.homa.payment.usecase.SequentialEventIds;
import com.homa.payment.usecase.exception.ReservationHoldNotFoundException;
import com.homa.payment.usecase.port.out.PaymentGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleReservationWithdrawnIETest {

    private final InMemoryPaymentHoldRepository holds = new InMemoryPaymentHoldRepository();
    private final RecordingPaymentGateway gateway = new RecordingPaymentGateway();
    private final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();
    private final RecordingPaymentIEPublisher publisher = new RecordingPaymentIEPublisher();
    private final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    private final FixedTimeSource time = new FixedTimeSource(WITHDRAWN_AT);
    private final HandleReservationWithdrawnIE handler = new HandleReservationWithdrawnIE(holds, gateway, ledger,
            publisher, processed, time, new SequentialEventIds());

    @Test
    @Tag("REQ-2")
    @Tag("PAY-7")
    @DisplayName("Aisyah withdraws on Monday 21:00 and gets RM200 back.")
    void withdrawnRequestIsRefundedInFullAtOnce() {
        PaymentHold hold = aisyahsRequestedHold();
        holds.given(hold);

        handler.handle(withdrawn("reservation-event-1"));

        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.REFUNDED);
        assertThat(hold.held()).isEqualTo(Money.myr(0));
        assertThat(holds.saves()).containsExactly(hold);
        assertThat(gateway.refunds()).containsExactly(new PaymentGateway.RefundRequest(
                "refund:withdrawn:" + RESERVATION_ID, ONLINE_BANKING.value(), Money.myr(200)));
        assertThat(processed.recorded()).containsExactly("reservation-event-1|payment.ReservationWithdrawn");
    }

    @Test
    @Tag("REQ-2")
    @Tag("PAY-7")
    @DisplayName("Aisyah is told her RM200 was sent: the event carries the reservation, the customer, the amount, "
            + "the reason and when it was sent.")
    void refundPaidIsPublishedFieldByField() {
        holds.given(aisyahsRequestedHold());

        handler.handle(withdrawn("reservation-event-1"));

        assertThat(publisher.published()).containsExactly(new RefundPaidIE("event-1", RESERVATION_ID,
                AISYAH_CUSTOMER_ID, Money.myr(200), "WITHDRAWN", WITHDRAWN_AT));
        assertThat(publisher.published().get(0).occurredAt()).isEqualTo(WITHDRAWN_AT);
    }

    @Test
    @Tag("REQ-2")
    @Tag("PAY-4")
    @DisplayName("Aisyah's RM200 goes back out of Homa's bank: nothing is left held for her.")
    void refundIsPostedToTheLedger() {
        holds.given(aisyahsRequestedHold());
        ledger.post(new LedgerEntry(LedgerMovement.PAYMENT_HELD, Money.myr(200), PAID_AT));

        handler.handle(withdrawn("reservation-event-1"));

        assertThat(ledger.entries()).last()
                .isEqualTo(new LedgerEntry(LedgerMovement.REFUND_SENT, Money.myr(200), WITHDRAWN_AT));
        assertThat(ledger.balanceOf(LedgerAccount.CUSTOMER_HELD)).isEqualTo(Money.myr(0));
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_BANK)).isEqualTo(Money.myr(0));
    }

    @Test
    @Tag("REQ-2")
    @DisplayName("A redelivered withdrawal refunds nothing a second time.")
    void redeliveredEventChangesNothing() {
        holds.given(aisyahsRequestedHold());
        handler.handle(withdrawn("reservation-event-1"));

        handler.handle(withdrawn("reservation-event-1"));

        assertThat(gateway.refunds()).hasSize(1);
        assertThat(holds.saves()).hasSize(1);
        assertThat(publisher.published()).hasSize(1);
        assertThat(ledger.entries()).hasSize(1);
    }

    @Test
    @Tag("REQ-2")
    @DisplayName("An event this handler already handled is not applied again, whatever state the hold is in.")
    void alreadyHandledEventIsNotApplied() {
        PaymentHold hold = aisyahsRequestedHold();
        holds.given(hold);
        processed.recordFirstHandling("reservation-event-1", HandleReservationWithdrawnIE.HANDLER);

        handler.handle(withdrawn("reservation-event-1"));

        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.HELD);
        assertThat(gateway.refunds()).isEmpty();
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("PAY-7")
    @DisplayName("A refund sent again after a failure carries the same idempotency key, so the gateway pays it once.")
    void refundKeyDependsOnlyOnTheReservation() {
        holds.given(aisyahsRequestedHold());
        handler.handle(withdrawn("reservation-event-1"));

        InMemoryPaymentHoldRepository retriedHolds = new InMemoryPaymentHoldRepository();
        retriedHolds.given(aisyahsRequestedHold());
        RecordingPaymentGateway retriedGateway = new RecordingPaymentGateway();
        new HandleReservationWithdrawnIE(retriedHolds, retriedGateway, new InMemoryLedgerRepository(),
                new RecordingPaymentIEPublisher(), new InMemoryProcessedMessageStore(), time,
                new SequentialEventIds()).handle(withdrawn("reservation-event-2"));

        assertThat(retriedGateway.refunds()).extracting(PaymentGateway.RefundRequest::idempotencyKey)
                .isEqualTo(gateway.refunds().stream().map(PaymentGateway.RefundRequest::idempotencyKey).toList());
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("A withdrawal for a hold that holds nothing refunds nothing, and the event is not retried forever.")
    void holdHoldingNothingRefundsNothing() {
        PaymentHold hold = aisyahsRequestedHold();
        hold.refundInFull();
        holds.given(hold);

        handler.handle(withdrawn("reservation-event-1"));

        assertThat(holds.saves()).isEmpty();
        assertThat(gateway.refunds()).isEmpty();
        assertThat(publisher.published()).isEmpty();
        assertThat(ledger.entries()).isEmpty();
        assertThat(processed.recorded()).hasSize(1);
    }

    @Test
    @Tag("REQ-2")
    @DisplayName("A withdrawal for a reservation whose payment Payment never linked is refused, so it is retried and "
            + "reported.")
    void withdrawalForAnUnknownReservationIsRefused() {
        assertThatThrownBy(() -> handler.handle(withdrawn("reservation-event-1")))
                .isInstanceOf(ReservationHoldNotFoundException.class)
                .hasMessageContaining(RESERVATION_ID);
        assertThat(gateway.refunds()).isEmpty();
    }
}
