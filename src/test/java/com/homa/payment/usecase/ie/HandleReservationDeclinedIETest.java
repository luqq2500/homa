package com.homa.payment.usecase.ie;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.DECLINED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ONLINE_BANKING;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVATION_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.aisyahsRequestedHold;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.declined;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.RefundPaidIE;
import com.homa.kernel.Money;
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

class HandleReservationDeclinedIETest {

    private final InMemoryPaymentHoldRepository holds = new InMemoryPaymentHoldRepository();
    private final RecordingPaymentGateway gateway = new RecordingPaymentGateway();
    private final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();
    private final RecordingPaymentIEPublisher publisher = new RecordingPaymentIEPublisher();
    private final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    private final FixedTimeSource time = new FixedTimeSource(DECLINED_AT);
    private final HandleReservationDeclinedIE handler = new HandleReservationDeclinedIE(holds, gateway, ledger,
            publisher, processed, time, new SequentialEventIds());

    @Test
    @Tag("REQ-3")
    @Tag("PAY-7")
    @DisplayName("Kemas Home declines because two helpers are on leave. Aisyah gets RM200 back.")
    void declinedRequestIsRefundedInFullAtOnce() {
        PaymentHold hold = aisyahsRequestedHold();
        holds.given(hold);

        handler.handle(declined("reservation-event-1"));

        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.REFUNDED);
        assertThat(holds.saves()).containsExactly(hold);
        assertThat(gateway.refunds()).containsExactly(new PaymentGateway.RefundRequest(
                "refund:declined:" + RESERVATION_ID, ONLINE_BANKING.value(), Money.myr(200)));
        assertThat(ledger.entries())
                .containsExactly(new LedgerEntry(LedgerMovement.REFUND_SENT, Money.myr(200), DECLINED_AT));
        assertThat(processed.recorded()).containsExactly("reservation-event-1|payment.ReservationDeclined");
    }

    @Test
    @Tag("REQ-3")
    @Tag("PAY-7")
    @DisplayName("Aisyah is told her RM200 was sent, and why.")
    void refundPaidIsPublishedFieldByField() {
        holds.given(aisyahsRequestedHold());

        handler.handle(declined("reservation-event-1"));

        assertThat(publisher.published()).containsExactly(new RefundPaidIE("event-1", RESERVATION_ID,
                AISYAH_CUSTOMER_ID, Money.myr(200), "DECLINED", DECLINED_AT));
    }

    @Test
    @Tag("REQ-3")
    @DisplayName("A redelivered decline refunds nothing a second time.")
    void redeliveredEventChangesNothing() {
        holds.given(aisyahsRequestedHold());
        handler.handle(declined("reservation-event-1"));

        handler.handle(declined("reservation-event-1"));

        assertThat(gateway.refunds()).hasSize(1);
        assertThat(holds.saves()).hasSize(1);
        assertThat(publisher.published()).hasSize(1);
        assertThat(ledger.entries()).hasSize(1);
    }

    @Test
    @Tag("PAY-7")
    @DisplayName("A refund sent again after a failure carries the same idempotency key, so the gateway pays it once.")
    void refundKeyDependsOnlyOnTheReservation() {
        holds.given(aisyahsRequestedHold());
        handler.handle(declined("reservation-event-1"));

        InMemoryPaymentHoldRepository retriedHolds = new InMemoryPaymentHoldRepository();
        retriedHolds.given(aisyahsRequestedHold());
        RecordingPaymentGateway retriedGateway = new RecordingPaymentGateway();
        new HandleReservationDeclinedIE(retriedHolds, retriedGateway, new InMemoryLedgerRepository(),
                new RecordingPaymentIEPublisher(), new InMemoryProcessedMessageStore(), time,
                new SequentialEventIds()).handle(declined("reservation-event-2"));

        assertThat(retriedGateway.refunds()).extracting(PaymentGateway.RefundRequest::idempotencyKey)
                .containsExactly("refund:declined:" + RESERVATION_ID);
        assertThat(gateway.refunds()).extracting(PaymentGateway.RefundRequest::idempotencyKey)
                .containsExactly("refund:declined:" + RESERVATION_ID);
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("A decline for a hold that holds nothing refunds nothing, and the event is not retried forever.")
    void holdHoldingNothingRefundsNothing() {
        PaymentHold hold = aisyahsRequestedHold();
        hold.refundInFull();
        holds.given(hold);

        handler.handle(declined("reservation-event-1"));

        assertThat(gateway.refunds()).isEmpty();
        assertThat(publisher.published()).isEmpty();
        assertThat(processed.recorded()).hasSize(1);
    }

    @Test
    @Tag("REQ-3")
    @DisplayName("A decline for a reservation whose payment Payment never linked is refused, so it is retried and "
            + "reported.")
    void declineForAnUnknownReservationIsRefused() {
        assertThatThrownBy(() -> handler.handle(declined("reservation-event-1")))
                .isInstanceOf(ReservationHoldNotFoundException.class)
                .hasMessageContaining(RESERVATION_ID);
        assertThat(gateway.refunds()).isEmpty();
    }
}
