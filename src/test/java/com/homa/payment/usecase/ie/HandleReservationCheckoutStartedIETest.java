package com.homa.payment.usecase.ie;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.AISYAH;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.CHECKOUT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.CHECKOUT_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.CHECKOUT_STARTED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.checkoutStarted;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.ReservationCheckoutStartedIE;
import com.homa.kernel.Money;
import com.homa.payment.domain.PaymentHold;
import com.homa.payment.domain.PaymentHoldStatus;
import com.homa.payment.usecase.InMemoryPaymentHoldRepository;
import com.homa.payment.usecase.InMemoryProcessedMessageStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleReservationCheckoutStartedIETest {

    private final InMemoryPaymentHoldRepository holds = new InMemoryPaymentHoldRepository();
    private final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    private final HandleReservationCheckoutStartedIE handler =
            new HandleReservationCheckoutStartedIE(holds, processed);

    @Test
    @Tag("RES-11")
    @Tag("PAY-1")
    @DisplayName("Aisyah starts paying for RM200: a pending hold opens, and nothing is held yet.")
    void checkoutOpensAPendingHold() {
        handler.handle(checkoutStarted("reservation-event-1"));

        PaymentHold hold = holds.findByCheckoutId(CHECKOUT).orElseThrow();
        assertThat(holds.saves()).containsExactly(hold);
        assertThat(hold.id().value()).isNotBlank();
        assertThat(hold.customerId()).isEqualTo(AISYAH);
        assertThat(hold.total()).isEqualTo(Money.myr(200));
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.PENDING);
        assertThat(hold.held()).isEqualTo(Money.myr(0));
        assertThat(processed.recorded()).containsExactly("reservation-event-1|payment.ReservationCheckoutStarted");
    }

    @Test
    @Tag("RES-11")
    @DisplayName("A redelivered checkout start opens no second hold.")
    void redeliveredEventChangesNothing() {
        handler.handle(checkoutStarted("reservation-event-1"));
        PaymentHold first = holds.findByCheckoutId(CHECKOUT).orElseThrow();

        handler.handle(checkoutStarted("reservation-event-1"));

        assertThat(holds.saves()).containsExactly(first);
        assertThat(holds.findByCheckoutId(CHECKOUT)).containsSame(first);
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("A total with a fraction of a sen opens no hold, and the event is not retried forever.")
    void totalPaymentCannotTakeOpensNothing() {
        handler.handle(new ReservationCheckoutStartedIE("reservation-event-1", CHECKOUT_ID, AISYAH_CUSTOMER_ID,
                KEMAS_HOME_PROVIDER_ID, Money.myr("39.375"), CHECKOUT_STARTED_AT));

        assertThat(holds.findByCheckoutId(CHECKOUT)).isEmpty();
        assertThat(holds.saves()).isEmpty();
        assertThat(processed.recorded()).hasSize(1);
    }
}
