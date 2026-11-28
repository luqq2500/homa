package com.homa.payment.domain;

import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.payment.domain.PaymentFixtures.AISYAH;
import static com.homa.payment.domain.PaymentFixtures.AISYAHS_CARD_RETRY;
import static com.homa.payment.domain.PaymentFixtures.AISYAHS_HOLD;
import static com.homa.payment.domain.PaymentFixtures.AISYAHS_ONLINE_BANKING;
import static com.homa.payment.domain.PaymentFixtures.CARD_RETRY_CONFIRMED_AT;
import static com.homa.payment.domain.PaymentFixtures.PAID_AT;
import static com.homa.payment.domain.PaymentFixtures.SLOW_ONLINE_BANKING_CONFIRMED_AT;
import static com.homa.payment.domain.PaymentFixtures.STANDARD_CHECKOUT;
import static com.homa.payment.domain.PaymentFixtures.STANDARD_RESERVATION;
import static com.homa.payment.domain.PaymentFixtures.aisyahsCheckoutAwaitingPayment;
import static com.homa.payment.domain.PaymentFixtures.aisyahsHeldPayment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.fixtures.HomaFixtures;
import com.homa.kernel.Money;
import com.homa.payment.domain.exception.AmountNotPayableException;
import com.homa.payment.domain.exception.CheckoutNotAwaitingPaymentException;
import com.homa.payment.domain.exception.NoPaymentToCompensateException;
import com.homa.payment.domain.exception.PaymentHoldAlreadyLinkedException;
import com.homa.payment.domain.exception.PaymentNotHeldException;
import com.homa.payment.domain.exception.RefundExceedsHeldAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PaymentHoldTest {

    @Test
    @Tag("PAY-1")
    @DisplayName("Aisyah's RM200 is held from Monday 20:00. Kemas Home can't receive it yet.")
    void customerPaymentIsHeldFromTheMomentItIsConfirmed() {
        PaymentHold hold = aisyahsCheckoutAwaitingPayment();

        PaymentConfirmation confirmation = hold.recordConfirmation(AISYAHS_ONLINE_BANKING, PAID_AT);

        assertThat(confirmation).isEqualTo(
                new PaymentConfirmation.Counted(AISYAHS_ONLINE_BANKING, Money.myr(200), PAID_AT));
        assertThat(confirmation.attemptId()).isEqualTo(AISYAHS_ONLINE_BANKING);
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.HELD);
        assertThat(hold.held()).isEqualTo(Money.myr(200));
        assertThat(hold.countedAttempt()).contains(AISYAHS_ONLINE_BANKING);
        assertThat(hold.heldSince()).contains(HomaFixtures.at(2026, 11, 30, 20, 0));
    }

    @Test
    @Tag("PAY-1")
    @DisplayName("Aisyah taps Pay: nothing is held until the gateway confirms her RM200.")
    void checkoutHoldsNothingUntilAPaymentIsConfirmed() {
        PaymentHold hold = aisyahsCheckoutAwaitingPayment();

        assertThat(hold.id()).isEqualTo(AISYAHS_HOLD);
        assertThat(hold.checkoutId()).isEqualTo(STANDARD_CHECKOUT);
        assertThat(hold.customerId()).isEqualTo(AISYAH);
        assertThat(hold.total()).isEqualTo(TOTAL_RESERVATION_FEE);
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.PENDING);
        assertThat(hold.held()).isEqualTo(Money.myr(0));
        assertThat(hold.countedAttempt()).isEmpty();
        assertThat(hold.heldSince()).isEmpty();
    }

    @Test
    @Tag("RES-12")
    @DisplayName("Aisyah taps Pay twice before any payment confirms: each attempt gets its own gateway reference.")
    void customerCanStartAttemptsWhileNoPaymentIsConfirmed() {
        PaymentHold hold = aisyahsCheckoutAwaitingPayment();

        PaymentAttemptId card = hold.startAttempt();
        PaymentAttemptId onlineBanking = hold.startAttempt();

        assertThat(card).isNotNull().isNotEqualTo(onlineBanking);
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.PENDING);
    }

    @Test
    @Tag("RES-12")
    @DisplayName("Aisyah's RM200 is held: she can't start another payment, which would only be refunded.")
    void heldCheckoutTakesNoFurtherPayment() {
        PaymentHold hold = aisyahsHeldPayment();

        assertThatThrownBy(hold::startAttempt)
                .isInstanceOf(CheckoutNotAwaitingPaymentException.class)
                .hasMessageContaining(AISYAHS_HOLD.value())
                .hasMessageContaining("HELD");
    }

    @Test
    @Tag("RES-11")
    @DisplayName("Aisyah's rejected checkout was refunded in full: it takes no further payment.")
    void refundedCheckoutTakesNoFurtherPayment() {
        PaymentHold hold = aisyahsHeldPayment();
        hold.refundRejectedCheckout();

        assertThatThrownBy(hold::startAttempt)
                .isInstanceOf(CheckoutNotAwaitingPaymentException.class)
                .hasMessageContaining("REFUNDED");
    }

    @Test
    @Tag("RES-12")
    @DisplayName("Aisyah's first card attempt fails and she retries: the card retry confirms at 20:04 and counts.")
    void failedAttemptChangesNothingAndTheRetryCounts() {
        PaymentHold hold = aisyahsCheckoutAwaitingPayment();

        PaymentConfirmation retry = hold.recordConfirmation(AISYAHS_CARD_RETRY, CARD_RETRY_CONFIRMED_AT);

        assertThat(retry).isInstanceOf(PaymentConfirmation.Counted.class);
        assertThat(hold.countedAttempt()).contains(AISYAHS_CARD_RETRY);
        assertThat(hold.heldSince()).contains(CARD_RETRY_CONFIRMED_AT);
        assertThat(hold.held()).isEqualTo(Money.myr(200));
    }

    @Test
    @Tag("RES-12")
    @Tag("PAY-7")
    @DisplayName("Aisyah's card retry confirms at 20:04 and her slow bank payment at 20:06. The 20:06 payment is "
            + "refunded.")
    void laterConfirmedPaymentForTheSameCheckoutIsRefundedInFullAtOnce() {
        PaymentHold hold = aisyahsCheckoutAwaitingPayment();
        hold.recordConfirmation(AISYAHS_CARD_RETRY, CARD_RETRY_CONFIRMED_AT);

        PaymentConfirmation slow = hold.recordConfirmation(AISYAHS_ONLINE_BANKING, SLOW_ONLINE_BANKING_CONFIRMED_AT);

        Refund refund = new Refund(AISYAHS_HOLD, AISYAH, AISYAHS_ONLINE_BANKING, Money.myr(200));
        assertThat(slow).isEqualTo(new PaymentConfirmation.RefundedInFull(refund));
        assertThat(slow.attemptId()).isEqualTo(AISYAHS_ONLINE_BANKING);
        assertThat(hold.countedAttempt()).contains(AISYAHS_CARD_RETRY);
        assertThat(hold.heldSince()).contains(CARD_RETRY_CONFIRMED_AT);
        assertThat(hold.held()).isEqualTo(Money.myr(200));
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.HELD);
    }

    @Test
    @Tag("RES-12")
    @DisplayName("The gateway reports Aisyah's 20:04 and 20:06 payments twice: nothing is held or refunded again.")
    void sameAttemptConfirmedAgainChangesNothing() {
        PaymentHold hold = aisyahsCheckoutAwaitingPayment();
        hold.recordConfirmation(AISYAHS_CARD_RETRY, CARD_RETRY_CONFIRMED_AT);
        hold.recordConfirmation(AISYAHS_ONLINE_BANKING, SLOW_ONLINE_BANKING_CONFIRMED_AT);

        assertThat(hold.recordConfirmation(AISYAHS_CARD_RETRY, SLOW_ONLINE_BANKING_CONFIRMED_AT))
                .isEqualTo(new PaymentConfirmation.AlreadyRecorded(AISYAHS_CARD_RETRY));
        assertThat(hold.recordConfirmation(AISYAHS_ONLINE_BANKING, SLOW_ONLINE_BANKING_CONFIRMED_AT))
                .isEqualTo(new PaymentConfirmation.AlreadyRecorded(AISYAHS_ONLINE_BANKING));
        assertThat(hold.heldSince()).contains(CARD_RETRY_CONFIRMED_AT);
        assertThat(hold.held()).isEqualTo(Money.myr(200));
    }

    @Test
    @Tag("RES-12")
    @DisplayName("Aisyah withdrew and got her RM200 back; her slow 20:06 payment then confirms and is refunded "
            + "in full too.")
    void laterConfirmedPaymentIsRefundedEvenAfterTheHeldPaymentWasRefunded() {
        PaymentHold hold = aisyahsCheckoutAwaitingPayment();
        hold.recordConfirmation(AISYAHS_CARD_RETRY, CARD_RETRY_CONFIRMED_AT);
        hold.refund(Money.myr(200));

        PaymentConfirmation slow = hold.recordConfirmation(AISYAHS_ONLINE_BANKING, SLOW_ONLINE_BANKING_CONFIRMED_AT);

        assertThat(slow).isEqualTo(new PaymentConfirmation.RefundedInFull(
                new Refund(AISYAHS_HOLD, AISYAH, AISYAHS_ONLINE_BANKING, Money.myr(200))));
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.REFUNDED);
        assertThat(hold.held()).isEqualTo(Money.myr(0));
    }

    @Test
    @Tag("PAY-4")
    @Tag("PAY-7")
    @DisplayName("Aisyah cancels on Friday 22:00. Her RM155 refund is sent straight away from her RM200: "
            + "RM45 stays held.")
    void refundComesOutOfTheHeldPaymentAtOnceAndLeavesTheRestHeld() {
        PaymentHold hold = aisyahsHeldPayment();

        Refund refund = hold.refund(Money.myr(155));

        assertThat(refund).isEqualTo(new Refund(AISYAHS_HOLD, AISYAH, AISYAHS_ONLINE_BANKING, Money.myr(155)));
        assertThat(hold.held()).isEqualTo(Money.myr(45));
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.HELD);
    }

    @Test
    @Tag("PAY-7")
    @Tag("RPT-9")
    @DisplayName("When Nurul leaves at 12:00, Aisyah's RM45 partial refund is sent at once.")
    void partialRefundIsReturnedAtOnce() {
        PaymentHold hold = aisyahsHeldPayment();

        Refund refund = hold.refund(Money.myr(45));

        assertThat(refund.amount()).isEqualTo(Money.myr(45));
        assertThat(refund.attemptId()).isEqualTo(AISYAHS_ONLINE_BANKING);
        assertThat(hold.held()).isEqualTo(Money.myr(155));
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
    void paymentNotYetConfirmedCannotBeRefunded() {
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
    @Tag("RES-11")
    @DisplayName("Aisyah and Mei both pay for Kemas Home on Saturday at 10:00. Mei's payment confirms first, "
            + "so Aisyah gets her RM200 back straight away.")
    void rejectedCheckoutIsRefundedInFullAtOnce() {
        PaymentHold hold = aisyahsHeldPayment();

        Refund refund = hold.refundRejectedCheckout();

        assertThat(refund).isEqualTo(new Refund(AISYAHS_HOLD, AISYAH, AISYAHS_ONLINE_BANKING, Money.myr(200)));
        assertThat(hold.held()).isEqualTo(Money.myr(0));
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.REFUNDED);
    }

    @Test
    @Tag("RES-11")
    @DisplayName("A rejected checkout is refunded once; a checkout with no confirmed payment has nothing to refund.")
    void rejectedCheckoutIsRefundedOnlyWhileThePaymentIsHeld() {
        PaymentHold refunded = aisyahsHeldPayment();
        refunded.refundRejectedCheckout();

        assertThatThrownBy(refunded::refundRejectedCheckout).isInstanceOf(PaymentNotHeldException.class);
        assertThatThrownBy(aisyahsCheckoutAwaitingPayment()::refundRejectedCheckout)
                .isInstanceOf(PaymentNotHeldException.class);
    }

    @Test
    @Tag("RES-7")
    @DisplayName("Aisyah pays RM200 on Monday 20:00 and her reservation is Requested: the payment is held for that "
            + "reservation.")
    void heldPaymentIsLinkedToTheRequestedReservation() {
        PaymentHold hold = aisyahsHeldPayment();
        assertThat(hold.reservationId()).isEmpty();

        hold.linkToReservation(STANDARD_RESERVATION);

        assertThat(hold.reservationId()).contains(STANDARD_RESERVATION);
    }

    @Test
    @Tag("RES-7")
    @DisplayName("The Requested event reaches Payment twice: the payment stays linked to the same reservation, even "
            + "after it was refunded.")
    void linkingTheSameReservationAgainChangesNothing() {
        PaymentHold hold = aisyahsHeldPayment();
        hold.linkToReservation(STANDARD_RESERVATION);
        hold.refundInFull();

        hold.linkToReservation(STANDARD_RESERVATION);

        assertThat(hold.reservationId()).contains(STANDARD_RESERVATION);
    }

    @Test
    @Tag("RES-7")
    @DisplayName("Aisyah's payment belongs to her Saturday reservation: it is never linked to another one.")
    void paymentBelongsToOneReservationOnly() {
        PaymentHold hold = aisyahsHeldPayment();
        hold.linkToReservation(STANDARD_RESERVATION);

        assertThatThrownBy(() -> hold.linkToReservation(new ReservationId("reservation-sunday")))
                .isInstanceOf(PaymentHoldAlreadyLinkedException.class)
                .hasMessageContaining(AISYAHS_HOLD.value())
                .hasMessageContaining(STANDARD_RESERVATION.value());
        assertThat(hold.reservationId()).contains(STANDARD_RESERVATION);
    }

    @Test
    @Tag("RES-7")
    @DisplayName("A reservation is Requested only once the payment is held: a checkout with no confirmed payment, "
            + "or one already refunded, is not linked.")
    void onlyAHeldPaymentIsLinkedToAReservation() {
        PaymentHold awaiting = aisyahsCheckoutAwaitingPayment();
        PaymentHold refunded = aisyahsHeldPayment();
        refunded.refundInFull();

        assertThatThrownBy(() -> awaiting.linkToReservation(STANDARD_RESERVATION))
                .isInstanceOf(PaymentNotHeldException.class);
        assertThatThrownBy(() -> refunded.linkToReservation(STANDARD_RESERVATION))
                .isInstanceOf(PaymentNotHeldException.class);
        assertThat(awaiting.reservationId()).isEmpty();
        assertThat(refunded.reservationId()).isEmpty();
    }

    @Test
    @Tag("RES-7")
    @DisplayName("A reservation to link is required.")
    void linkingNoReservationIsRefused() {
        assertThatThrownBy(() -> aisyahsHeldPayment().linkToReservation(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @Tag("REQ-2")
    @Tag("PAY-7")
    @DisplayName("Aisyah withdraws on Monday 21:00 and gets RM200 back.")
    void withdrawnRequestIsRefundedInFullAtOnce() {
        PaymentHold hold = aisyahsHeldPayment();

        Refund refund = hold.refundInFull();

        assertThat(refund).isEqualTo(new Refund(AISYAHS_HOLD, AISYAH, AISYAHS_ONLINE_BANKING, Money.myr(200)));
        assertThat(hold.held()).isEqualTo(Money.myr(0));
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.REFUNDED);
    }

    @Test
    @Tag("REQ-3")
    @Tag("PAY-4")
    @DisplayName("Everything still held goes back: after RM45 was refunded, RM155 is.")
    void refundInFullReturnsWhatIsStillHeld() {
        PaymentHold hold = aisyahsHeldPayment();
        hold.refund(Money.myr(45));

        Refund refund = hold.refundInFull();

        assertThat(refund.amount()).isEqualTo(Money.myr(155));
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.REFUNDED);
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("A request is refunded in full once; a checkout with no confirmed payment has nothing to refund.")
    void refundInFullNeedsAHeldPayment() {
        PaymentHold refunded = aisyahsHeldPayment();
        refunded.refundInFull();

        assertThatThrownBy(refunded::refundInFull).isInstanceOf(PaymentNotHeldException.class);
        assertThatThrownBy(aisyahsCheckoutAwaitingPayment()::refundInFull)
                .isInstanceOf(PaymentNotHeldException.class);
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("After a provider no-show, Aisyah gets RM200 back plus RM20 from Homa: the compensation does not come "
            + "out of what is held.")
    void compensationIsPaidByHomaAndLeavesTheHeldPaymentAlone() {
        PaymentHold hold = aisyahsHeldPayment();
        hold.refundInFull();

        Compensation compensation = hold.compensate(Money.myr(20));

        assertThat(compensation)
                .isEqualTo(new Compensation(AISYAHS_HOLD, AISYAH, AISYAHS_ONLINE_BANKING, Money.myr(20)));
        assertThat(hold.held()).isEqualTo(Money.myr(0));
        assertThat(hold.status()).isEqualTo(PaymentHoldStatus.REFUNDED);
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("Compensation can be paid while the payment is still held too, and goes back to the payment that "
            + "counted, not to a later one refunded in full.")
    void compensationGoesToThePaymentThatCounted() {
        PaymentHold hold = aisyahsHeldPayment();
        hold.recordConfirmation(AISYAHS_CARD_RETRY, CARD_RETRY_CONFIRMED_AT);

        Compensation compensation = hold.compensate(Money.myr(20));

        assertThat(compensation.attemptId()).isEqualTo(AISYAHS_ONLINE_BANKING);
        assertThat(hold.held()).isEqualTo(Money.myr(200));
    }

    @Test
    @Tag("PAY-4")
    @Tag("PAY-8")
    @DisplayName("Compensation is a positive amount in whole sen, and needs a confirmed payment to be paid to.")
    void compensationNeedsAPayableAmountAndAConfirmedPayment() {
        PaymentHold hold = aisyahsHeldPayment();

        assertThatThrownBy(() -> hold.compensate(Money.myr(0)))
                .isInstanceOf(AmountNotPayableException.class)
                .hasMessageContaining("compensation");
        assertThatThrownBy(() -> hold.compensate(Money.myr("20.005"))).isInstanceOf(AmountNotPayableException.class);
        assertThatThrownBy(() -> aisyahsCheckoutAwaitingPayment().compensate(Money.myr(20)))
                .isInstanceOf(NoPaymentToCompensateException.class)
                .hasMessageContaining(AISYAHS_HOLD.value());
    }
}
