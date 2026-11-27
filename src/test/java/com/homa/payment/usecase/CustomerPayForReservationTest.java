package com.homa.payment.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.CHECKOUT_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.MEI_CUSTOMER_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.aisyahsHeldPayment;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.aisyahsPendingHold;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.payment.domain.PaymentHoldStatus;
import com.homa.payment.domain.exception.CheckoutNotAwaitingPaymentException;
import com.homa.payment.usecase.dto.PayForReservationRequest;
import com.homa.payment.usecase.dto.PayForReservationResponse;
import com.homa.payment.usecase.exception.PaymentHoldNotFoundException;
import com.homa.payment.usecase.exception.PaymentNotAllowedException;
import com.homa.payment.usecase.port.out.PaymentGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerPayForReservationTest {

    private final InMemoryPaymentHoldRepository holds = new InMemoryPaymentHoldRepository();
    private final RecordingPaymentGateway gateway = new RecordingPaymentGateway();
    private final CustomerPayForReservation payer = new CustomerPayForReservation(holds, gateway);

    @Test
    @Tag("PAY-1")
    @DisplayName("Aisyah taps Pay: the gateway opens a RM200 session whose reference is her new payment attempt.")
    void payingOpensAGatewaySessionForTheCheckoutTotal() {
        holds.given(aisyahsPendingHold());

        PayForReservationResponse response = payer.pay(new PayForReservationRequest(AISYAH_CUSTOMER_ID, CHECKOUT_ID));

        String reference = response.paymentReference();
        assertThat(reference).isNotBlank();
        assertThat(gateway.sessions()).containsExactly(new PaymentGateway.SessionRequest(
                "payment-attempt:" + reference, reference, CHECKOUT_ID, Money.myr(200)));
        assertThat(response).isEqualTo(new PayForReservationResponse(CHECKOUT_ID, reference,
                "session-" + reference, "https://gateway.test/pay/" + reference, Money.myr(200)));
        assertThat(holds.saves()).isEmpty();
        assertThat(holds.findByCheckoutId(PaymentUsecaseFixtures.CHECKOUT).orElseThrow().status())
                .isEqualTo(PaymentHoldStatus.PENDING);
    }

    @Test
    @Tag("RES-12")
    @DisplayName("Aisyah's card attempt fails and she retries by online banking: each attempt has its own reference.")
    void everyAttemptHasItsOwnReference() {
        holds.given(aisyahsPendingHold());
        PayForReservationRequest request = new PayForReservationRequest(AISYAH_CUSTOMER_ID, CHECKOUT_ID);

        String card = payer.pay(request).paymentReference();
        String onlineBanking = payer.pay(request).paymentReference();

        assertThat(card).isNotEqualTo(onlineBanking);
        assertThat(gateway.sessions()).extracting(PaymentGateway.SessionRequest::idempotencyKey)
                .containsExactly("payment-attempt:" + card, "payment-attempt:" + onlineBanking);
    }

    @Test
    @Tag("RES-12")
    @DisplayName("Aisyah's RM200 is already held: she can't pay again.")
    void heldCheckoutTakesNoFurtherPayment() {
        holds.given(aisyahsHeldPayment());

        assertThatThrownBy(() -> payer.pay(new PayForReservationRequest(AISYAH_CUSTOMER_ID, CHECKOUT_ID)))
                .isInstanceOf(CheckoutNotAwaitingPaymentException.class);
        assertThat(gateway.sessions()).isEmpty();
    }

    @Test
    @DisplayName("Mei can't pay for the checkout Aisyah started.")
    void onlyTheCustomerWhoStartedTheCheckoutPays() {
        holds.given(aisyahsPendingHold());

        assertThatThrownBy(() -> payer.pay(new PayForReservationRequest(MEI_CUSTOMER_ID, CHECKOUT_ID)))
                .isInstanceOf(PaymentNotAllowedException.class)
                .hasMessageContaining(MEI_CUSTOMER_ID)
                .hasMessageContaining(CHECKOUT_ID);
        assertThat(gateway.sessions()).isEmpty();
    }

    @Test
    @DisplayName("A checkout Payment hasn't opened a hold for can't be paid yet.")
    void checkoutWithoutAHoldCannotBePaid() {
        assertThatThrownBy(() -> payer.pay(new PayForReservationRequest(AISYAH_CUSTOMER_ID, CHECKOUT_ID)))
                .isInstanceOf(PaymentHoldNotFoundException.class)
                .hasMessageContaining(CHECKOUT_ID);
        assertThat(gateway.sessions()).isEmpty();
    }
}
