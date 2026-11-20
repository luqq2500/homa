package com.homa.payment.domain;

import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.payment.domain.PaymentFixtures.AISYAH;
import static com.homa.payment.domain.PaymentFixtures.AISYAHS_HOLD;
import static com.homa.payment.domain.PaymentFixtures.PAID_AT;
import static com.homa.payment.domain.PaymentFixtures.STANDARD_CHECKOUT;
import static com.homa.payment.domain.PaymentFixtures.aisyahsCheckoutAwaitingPayment;
import static com.homa.payment.domain.PaymentFixtures.aisyahsHeldPayment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.fixtures.HomaFixtures;
import com.homa.kernel.Money;
import com.homa.payment.domain.exception.AmountNotPayableException;
import com.homa.payment.domain.exception.PaymentNotHeldException;
import com.homa.payment.domain.exception.PaymentNotPendingException;
import com.homa.payment.domain.exception.RefundExceedsHeldAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PaymentHoldTest {

    @Test
    @Tag("PAY-1")
    @DisplayName("Aisyah's RM200 is held from Monday 20:00. Kemas Home can't receive it yet.")
    void customerPaymentIsHeldFromTheMomentTheReservationIsRequested() {
        PaymentHold hold = aisyahsCheckoutAwaitingPayment();

        hold.hold(PAID_AT);

        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.HELD);
        assertThat(hold.held()).isEqualTo(Money.myr(200));
        assertThat(hold.heldSince()).contains(HomaFixtures.at(2026, 11, 30, 20, 0));
    }

    @Test
    @Tag("PAY-1")
    @DisplayName("Aisyah taps Pay: nothing is held until the gateway secures her RM200.")
    void checkoutHoldsNothingUntilThePaymentIsSecured() {
        PaymentHold hold = aisyahsCheckoutAwaitingPayment();

        assertThat(hold.id()).isEqualTo(AISYAHS_HOLD);
        assertThat(hold.checkoutId()).isEqualTo(STANDARD_CHECKOUT);
        assertThat(hold.customerId()).isEqualTo(AISYAH);
        assertThat(hold.total()).isEqualTo(TOTAL_RESERVATION_FEE);
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.PENDING);
        assertThat(hold.held()).isEqualTo(Money.myr(0));
        assertThat(hold.heldSince()).isEmpty();
    }

    @Test
    @Tag("PAY-1")
    @DisplayName("The gateway confirms Aisyah's payment twice: the second confirmation is refused.")
    void paymentIsHeldOnlyOnce() {
        PaymentHold hold = aisyahsHeldPayment();

        assertThatThrownBy(() -> hold.hold(PAID_AT.plusSeconds(60)))
                .isInstanceOf(PaymentNotPendingException.class)
                .hasMessageContaining("HELD");
        assertThat(hold.heldSince()).contains(PAID_AT);
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("Aisyah is refunded RM155 from her RM200: RM45 stays held.")
    void refundComesOutOfTheHeldPaymentAndLeavesTheRestHeld() {
        PaymentHold hold = aisyahsHeldPayment();

        Refund refund = hold.refund(Money.myr(155));

        assertThat(refund).isEqualTo(new Refund(AISYAHS_HOLD, AISYAH, Money.myr(155)));
        assertThat(hold.held()).isEqualTo(Money.myr(45));
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.HELD);
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("Aisyah's RM200 is held: a RM200.01 refund is refused, a RM200 refund returns everything.")
    void refundIsNeverMoreThanIsHeld() {
        PaymentHold hold = aisyahsHeldPayment();

        assertThatThrownBy(() -> hold.refund(Money.myr("200.01")))
                .isInstanceOf(RefundExceedsHeldAmountException.class)
                .hasMessageContaining("200.01");
        assertThat(hold.held()).isEqualTo(Money.myr(200));

        hold.refund(Money.myr(200));

        assertThat(hold.held()).isEqualTo(Money.myr(0));
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.REFUNDED);
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("Aisyah's RM200 was refunded in full: nothing more can be refunded.")
    void fullyRefundedPaymentHasNothingLeftToRefund() {
        PaymentHold hold = aisyahsHeldPayment();
        hold.refund(Money.myr(200));

        assertThatThrownBy(() -> hold.refund(Money.myr(1)))
                .isInstanceOf(PaymentNotHeldException.class)
                .hasMessageContaining("REFUNDED");
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("Aisyah has tapped Pay but the gateway hasn't confirmed: there is nothing to refund yet.")
    void paymentNotYetSecuredCannotBeRefunded() {
        PaymentHold hold = aisyahsCheckoutAwaitingPayment();

        assertThatThrownBy(() -> hold.refund(Money.myr(45)))
                .isInstanceOf(PaymentNotHeldException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("Settlement lines arrive rounded to the sen: RM39.375, zero and negative refunds are refused.")
    void refundIsAPositiveAmountInWholeSen() {
        PaymentHold hold = aisyahsHeldPayment();

        assertThatThrownBy(() -> hold.refund(Money.myr("39.375")))
                .isInstanceOf(AmountNotPayableException.class)
                .hasMessageContaining("39.375");
        assertThatThrownBy(() -> hold.refund(Money.myr(0))).isInstanceOf(AmountNotPayableException.class);
        assertThatThrownBy(() -> hold.refund(Money.myr(-45))).isInstanceOf(AmountNotPayableException.class);
        assertThat(hold.refund(Money.myr("39.38")).amount()).isEqualTo(Money.myr("39.38"));
        assertThat(hold.held()).isEqualTo(Money.myr("160.62"));
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("A checkout total of RM0 or RM200.005 is refused; RM200 is accepted.")
    void checkoutTotalIsAPositiveAmountInWholeSen() {
        assertThatThrownBy(() -> PaymentHold.openForCheckout(AISYAHS_HOLD, STANDARD_CHECKOUT, AISYAH, Money.myr(0)))
                .isInstanceOf(AmountNotPayableException.class)
                .hasMessageContaining("checkout total");
        assertThatThrownBy(() -> PaymentHold.openForCheckout(
                AISYAHS_HOLD, STANDARD_CHECKOUT, AISYAH, Money.myr("200.005")))
                .isInstanceOf(AmountNotPayableException.class);
    }

    @Test
    @Tag("RES-12")
    @DisplayName("Another customer booked the slot at 20:15, so Aisyah's 20:20 payment is refunded: she gets RM200 back straight away.")
    void rejectedCheckoutIsRefundedInFullAtOnce() {
        PaymentHold hold = aisyahsHeldPayment();

        Refund refund = hold.refundRejectedCheckout();

        assertThat(refund).isEqualTo(new Refund(AISYAHS_HOLD, AISYAH, Money.myr(200)));
        assertThat(hold.held()).isEqualTo(Money.myr(0));
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.REFUNDED);
    }

    @Test
    @Tag("RES-12")
    @DisplayName("A rejected checkout is refunded once; an unpaid checkout has nothing to refund.")
    void rejectedCheckoutIsRefundedOnlyWhileThePaymentIsHeld() {
        PaymentHold refunded = aisyahsHeldPayment();
        refunded.refundRejectedCheckout();

        assertThatThrownBy(refunded::refundRejectedCheckout).isInstanceOf(PaymentNotHeldException.class);
        assertThatThrownBy(aisyahsCheckoutAwaitingPayment()::refundRejectedCheckout)
                .isInstanceOf(PaymentNotHeldException.class);
    }
}
