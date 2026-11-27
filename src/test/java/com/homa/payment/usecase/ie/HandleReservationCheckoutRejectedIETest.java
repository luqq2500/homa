package com.homa.payment.usecase.ie;

import static com.homa.payment.usecase.PaymentUsecaseFixtures.CHECKOUT_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ONLINE_BANKING;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.aisyahsHeldPayment;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.aisyahsPendingHold;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.checkoutRejected;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.payment.domain.PaymentHold;
import com.homa.payment.domain.PaymentHoldStatus;
import com.homa.payment.usecase.InMemoryPaymentHoldRepository;
import com.homa.payment.usecase.InMemoryProcessedMessageStore;
import com.homa.payment.usecase.RecordingPaymentGateway;
import com.homa.payment.usecase.exception.PaymentHoldNotFoundException;
import com.homa.payment.usecase.port.out.PaymentGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleReservationCheckoutRejectedIETest {

    private final InMemoryPaymentHoldRepository holds = new InMemoryPaymentHoldRepository();
    private final RecordingPaymentGateway gateway = new RecordingPaymentGateway();
    private final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    private final HandleReservationCheckoutRejectedIE handler =
            new HandleReservationCheckoutRejectedIE(holds, gateway, processed);

    @Test
    @Tag("RES-11")
    @Tag("PAY-7")
    @DisplayName("Mei's payment took the time first: Aisyah's RM200 is refunded in full, at once.")
    void rejectedCheckoutIsRefundedInFullAtOnce() {
        PaymentHold hold = aisyahsHeldPayment();
        holds.given(hold);

        handler.handle(checkoutRejected("reservation-event-1"));

        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.REFUNDED);
        assertThat(hold.held()).isEqualTo(Money.myr(0));
        assertThat(holds.saves()).containsExactly(hold);
        assertThat(gateway.refunds()).containsExactly(new PaymentGateway.RefundRequest(
                "refund:rejected-checkout:" + CHECKOUT_ID, ONLINE_BANKING.value(), Money.myr(200)));
        assertThat(processed.recorded()).containsExactly("reservation-event-1|payment.ReservationCheckoutRejected");
    }

    @Test
    @Tag("RES-11")
    @DisplayName("A redelivered rejection refunds nothing a second time.")
    void redeliveredEventChangesNothing() {
        holds.given(aisyahsHeldPayment());
        handler.handle(checkoutRejected("reservation-event-1"));

        handler.handle(checkoutRejected("reservation-event-1"));

        assertThat(gateway.refunds()).hasSize(1);
        assertThat(holds.saves()).hasSize(1);
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("A rejected checkout that holds nothing refunds nothing, and the event is not retried forever.")
    void checkoutHoldingNothingRefundsNothing() {
        PaymentHold hold = aisyahsPendingHold();
        holds.given(hold);

        handler.handle(checkoutRejected("reservation-event-1"));

        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.PENDING);
        assertThat(holds.saves()).isEmpty();
        assertThat(gateway.refunds()).isEmpty();
        assertThat(processed.recorded()).hasSize(1);
    }

    @Test
    @DisplayName("A rejection for a checkout Payment never opened is refused, so it is retried and reported.")
    void rejectionForAnUnknownCheckoutIsRefused() {
        assertThatThrownBy(() -> handler.handle(checkoutRejected("reservation-event-1")))
                .isInstanceOf(PaymentHoldNotFoundException.class)
                .hasMessageContaining(CHECKOUT_ID);
        assertThat(gateway.refunds()).isEmpty();
    }
}
