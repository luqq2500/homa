package com.homa.payment.usecase;

import static com.homa.payment.usecase.PaymentUsecaseFixtures.CARD;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.CHECKOUT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.CHECKOUT_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ONLINE_BANKING;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PAID_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.SECOND_PAYMENT_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.aisyahsPendingHold;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.confirmed;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.refused;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.PaymentFailedIE;
import com.homa.ie.events.PaymentHeldIE;
import com.homa.kernel.Money;
import com.homa.payment.domain.PaymentHold;
import com.homa.payment.domain.PaymentHoldStatus;
import com.homa.payment.usecase.dto.ConfirmGatewayPaymentResponse;
import com.homa.payment.usecase.exception.PaymentHoldNotFoundException;
import com.homa.payment.usecase.port.out.PaymentGateway;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ConfirmGatewayPaymentTest {

    private static final Instant WEBHOOK_AT = Instant.parse("2026-11-30T12:00:03Z");

    private final InMemoryPaymentHoldRepository holds = new InMemoryPaymentHoldRepository();
    private final RecordingPaymentGateway gateway = new RecordingPaymentGateway();
    private final RecordingPaymentIEPublisher publisher = new RecordingPaymentIEPublisher();
    private final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    private final ConfirmGatewayPayment confirmer = new ConfirmGatewayPayment(holds, gateway, publisher, processed,
            new FixedTimeSource(WEBHOOK_AT), new SequentialEventIds());
    private final PaymentHold hold = aisyahsPendingHold();

    ConfirmGatewayPaymentTest() {
        holds.given(hold);
    }

    @Test
    @Tag("PAY-1")
    @Tag("EXT-2")
    @DisplayName("The gateway confirms Aisyah's online banking at 20:00: her RM200 is held and Reservation is told.")
    void firstConfirmedPaymentIsHeldAndPublished() {
        ConfirmGatewayPaymentResponse response = confirmer.confirm(confirmed("gw-evt-1", ONLINE_BANKING, PAID_AT));

        assertThat(response.outcome()).isEqualTo("HELD");
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.HELD);
        assertThat(hold.countedAttempt()).contains(ONLINE_BANKING);
        assertThat(holds.saves()).containsExactly(hold);
        assertThat(publisher.published()).containsExactly(
                new PaymentHeldIE("event-1", CHECKOUT_ID, Money.myr(200), PAID_AT, WEBHOOK_AT));
        assertThat(gateway.refunds()).isEmpty();
        assertThat(processed.recorded()).containsExactly("gw-evt-1|payment.GatewayPaymentWebhook");
    }

    @Test
    @Tag("EXT-2")
    @DisplayName("The gateway sends the same confirmation twice: the second does nothing.")
    void duplicateWebhookDoesNothing() {
        confirmer.confirm(confirmed("gw-evt-1", ONLINE_BANKING, PAID_AT));
        publisher.clear();

        ConfirmGatewayPaymentResponse response = confirmer.confirm(confirmed("gw-evt-1", ONLINE_BANKING, PAID_AT));

        assertThat(response.outcome()).isEqualTo("DUPLICATE");
        assertThat(holds.saves()).hasSize(1);
        assertThat(publisher.published()).isEmpty();
        assertThat(gateway.refunds()).isEmpty();
    }

    @Test
    @Tag("RES-12")
    @Tag("PAY-7")
    @DisplayName("Aisyah's card payment confirms at 20:02, after her online banking: the RM200 card payment is "
            + "refunded at once and never published.")
    void laterConfirmedPaymentIsRefundedAtOnce() {
        confirmer.confirm(confirmed("gw-evt-1", ONLINE_BANKING, PAID_AT));
        publisher.clear();

        ConfirmGatewayPaymentResponse response = confirmer.confirm(confirmed("gw-evt-2", CARD, SECOND_PAYMENT_AT));

        assertThat(response.outcome()).isEqualTo("REFUNDED");
        assertThat(gateway.refunds()).containsExactly(new PaymentGateway.RefundRequest(
                "refund:duplicate-payment:" + CARD.value(), CARD.value(), Money.myr(200)));
        assertThat(publisher.published()).isEmpty();
        assertThat(holds.saves()).containsExactly(hold, hold);
        assertThat(hold.countedAttempt()).contains(ONLINE_BANKING);
        assertThat(hold.held()).isEqualTo(Money.myr(200));
    }

    @Test
    @Tag("RES-12")
    @DisplayName("The gateway reports Aisyah's confirmed online banking again under a new notification: nothing is "
            + "held, refunded or published again.")
    void sameAttemptConfirmedAgainDoesNothing() {
        confirmer.confirm(confirmed("gw-evt-1", ONLINE_BANKING, PAID_AT));
        publisher.clear();

        ConfirmGatewayPaymentResponse response =
                confirmer.confirm(confirmed("gw-evt-3", ONLINE_BANKING, SECOND_PAYMENT_AT));

        assertThat(response.outcome()).isEqualTo("ALREADY_RECORDED");
        assertThat(holds.saves()).hasSize(1);
        assertThat(publisher.published()).isEmpty();
        assertThat(gateway.refunds()).isEmpty();
        assertThat(hold.heldSince()).contains(PAID_AT);
    }

    @Test
    @Tag("RES-11")
    @DisplayName("Aisyah's card is declined: Reservation is told, nothing is held, and she can retry.")
    void refusedPaymentIsPublishedAndChangesNothing() {
        ConfirmGatewayPaymentResponse response = confirmer.confirm(refused("gw-evt-1", CARD));

        assertThat(response.outcome()).isEqualTo("FAILED");
        assertThat(publisher.published()).containsExactly(
                new PaymentFailedIE("event-1", CHECKOUT_ID, "CARD_DECLINED", WEBHOOK_AT));
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.PENDING);
        assertThat(holds.saves()).isEmpty();
        assertThat(gateway.refunds()).isEmpty();
        assertThat(hold.startAttempt()).isNotNull();
    }

    @Test
    @DisplayName("A webhook for a checkout Payment never opened is refused, so the gateway retries it.")
    void webhookForAnUnknownCheckoutIsRefused() {
        InMemoryPaymentHoldRepository empty = new InMemoryPaymentHoldRepository();
        ConfirmGatewayPayment onEmpty = new ConfirmGatewayPayment(empty, gateway, publisher, processed,
                new FixedTimeSource(WEBHOOK_AT), new SequentialEventIds());

        assertThatThrownBy(() -> onEmpty.confirm(confirmed("gw-evt-1", ONLINE_BANKING, PAID_AT)))
                .isInstanceOf(PaymentHoldNotFoundException.class)
                .hasMessageContaining(CHECKOUT.value());
        assertThat(publisher.published()).isEmpty();
    }
}
