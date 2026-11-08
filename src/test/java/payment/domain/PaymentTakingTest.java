package payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static payment.domain.PaymentBuilders.NOW;
import static payment.domain.PaymentBuilders.aHeldFpxPaymentOf;
import static payment.domain.PaymentBuilders.aHeldPaymentOf;
import static payment.domain.PaymentBuilders.anAuthorisedCardPaymentOf;
import static payment.domain.PaymentBuilders.rm;

import kernel.Money;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import payment.domain.PaymentInstruction.Kind;
import payment.domain.exception.InvalidPaymentAmountException;
import payment.domain.exception.InvalidPaymentStateException;
import payment.domain.exception.PaymentAlreadyAcceptedException;
import payment.domain.exception.PaymentDetailRequiredException;

/** Feature: Taking payment. */
class PaymentTakingTest {

    @Nested
    class AtRequest {

        @Test
        void aCardIsAuthorisedNotChargedAtRequest() {
            Payment payment = anAuthorisedCardPaymentOf("230.00");

            assertThat(payment.state()).isEqualTo(PaymentState.AUTHORISED);
            assertThat(payment.amount()).isEqualTo(rm("230.00"));
            assertThat(payment.collected()).isEqualTo(rm("0.00"));
            assertThat(payment.method()).isEqualTo(PaymentMethod.CARD);
            assertThat(payment.reservationId()).isEqualTo("reservation-1");
            assertThat(payment.id()).isNotBlank();
            assertThat(payment.isAccepted()).isFalse();
        }

        @Test
        void fpxIsPaidUpfront() {
            Payment payment = aHeldFpxPaymentOf("230.00");

            assertThat(payment.state()).isEqualTo(PaymentState.HELD);
            assertThat(payment.amount()).isEqualTo(rm("230.00"));
            assertThat(payment.collected()).isEqualTo(rm("230.00"));
            assertThat(payment.method()).isEqualTo(PaymentMethod.FPX);
        }

        @Test
        void aPaymentIsForAReservationAndAnAmountAboveZero() {
            assertThatThrownBy(() -> Payment.authoriseCard(" ", rm("230"), NOW)).isInstanceOf(PaymentDetailRequiredException.class);
            assertThatThrownBy(() -> Payment.authoriseCard("reservation-1", rm("0"), NOW))
                    .isInstanceOf(InvalidPaymentAmountException.class);
            assertThatThrownBy(() -> Payment.payUpfrontByFpx("reservation-1", rm("-1"), NOW))
                    .isInstanceOf(InvalidPaymentAmountException.class);
            assertThatThrownBy(() -> Payment.authoriseCard("reservation-1", rm("230"), null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Payment.payUpfrontByFpx("reservation-1", rm("230"), null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class OnAcceptance {

        @Test
        void aCardIsChargedWhenTheProviderAccepts() {
            Payment payment = anAuthorisedCardPaymentOf("230.00");

            payment.capture(NOW);

            assertThat(payment.state()).isEqualTo(PaymentState.HELD);
            assertThat(payment.collected()).isEqualTo(rm("230.00"));
            assertThat(payment.isAccepted()).isTrue();
        }

        @Test
        void anFpxPaymentIsKeptForTheBookingWhenTheProviderAccepts() {
            Payment payment = aHeldFpxPaymentOf("230.00");

            payment.capture(NOW);

            assertThat(payment.state()).isEqualTo(PaymentState.HELD);
            assertThat(payment.collected()).isEqualTo(rm("230.00"));
            assertThat(payment.isAccepted()).isTrue();
        }

        @Test
        void aPaymentIsCapturedOnce() {
            assertThatThrownBy(() -> aHeldPaymentOf("230.00").capture(NOW)).isInstanceOf(PaymentAlreadyAcceptedException.class);
            assertThatThrownBy(() -> aHeldPaymentOf("230.00").capture(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class EndingBeforeAcceptance {

        @Test
        void aDeclinedCardRequestReleasesTheAuthorisationAndRefundsNothing() {
            Payment payment = anAuthorisedCardPaymentOf("230.00");

            PaymentInstruction instruction = payment.cancelBeforeAcceptance(NOW);

            assertThat(instruction).isEqualTo(new PaymentInstruction(Kind.RELEASE_AUTHORISATION, rm("230.00"), NOW));
            assertThat(payment.refunded()).isEqualTo(rm("0.00"));
            assertThat(payment.state()).isEqualTo(PaymentState.AUTHORISATION_RELEASED);
            assertThat(payment.isFullyAccounted()).isTrue();
        }

        @Test
        void aDeclinedFpxRequestIsRefundedInFull() {
            Payment payment = aHeldFpxPaymentOf("230.00");

            PaymentInstruction instruction = payment.cancelBeforeAcceptance(NOW);

            assertThat(instruction).isEqualTo(new PaymentInstruction(Kind.REFUND_CUSTOMER, rm("230.00"), NOW));
            assertThat(payment.refunded()).isEqualTo(rm("230.00"));
            assertThat(payment.state()).isEqualTo(PaymentState.REFUNDED);
            assertThat(payment.isFullyAccounted()).isTrue();
        }

        @Test
        void anAcceptedPaymentIsNotEndedAsARequest() {
            assertThatThrownBy(() -> aHeldPaymentOf("230.00").cancelBeforeAcceptance(NOW))
                    .isInstanceOf(PaymentAlreadyAcceptedException.class);
        }

        @Test
        void anEndedRequestCannotBeEndedAgainCapturedOrEdited() {
            Payment card = anAuthorisedCardPaymentOf("230.00");
            card.cancelBeforeAcceptance(NOW);
            Payment fpx = aHeldFpxPaymentOf("230.00");
            fpx.cancelBeforeAcceptance(NOW);

            assertThatThrownBy(() -> card.cancelBeforeAcceptance(NOW)).isInstanceOf(InvalidPaymentStateException.class);
            assertThatThrownBy(() -> fpx.cancelBeforeAcceptance(NOW)).isInstanceOf(InvalidPaymentStateException.class);
            assertThatThrownBy(() -> card.capture(NOW)).isInstanceOf(InvalidPaymentStateException.class);
            assertThatThrownBy(() -> fpx.adjustTotal(rm("299"))).isInstanceOf(InvalidPaymentStateException.class);
        }
    }

    @Nested
    class EditingTheRequest {

        @Test
        void editingAnFpxRequestUpwardCollectsTheDifference() {
            Payment payment = aHeldFpxPaymentOf("230.00");

            Money difference = payment.adjustTotal(rm("299.00"));

            assertThat(difference).isEqualTo(rm("69.00"));
            assertThat(payment.amount()).isEqualTo(rm("299.00"));
            assertThat(payment.collected()).isEqualTo(rm("299.00"));
            assertThat(payment.refunded()).isEqualTo(rm("0.00"));
        }

        @Test
        void editingAnFpxRequestDownwardRefundsTheDifference() {
            Payment payment = aHeldFpxPaymentOf("230.00");

            Money difference = payment.adjustTotal(rm("126.50"));

            assertThat(difference).isEqualTo(rm("-103.50"));
            assertThat(payment.amount()).isEqualTo(rm("126.50"));
            assertThat(payment.collected()).isEqualTo(rm("230.00"));
            assertThat(payment.refunded()).isEqualTo(rm("103.50"));
        }

        @Test
        void editingACardRequestMovesTheAuthorisation() {
            Payment payment = anAuthorisedCardPaymentOf("230.00");

            assertThat(payment.adjustTotal(rm("299.00"))).isEqualTo(rm("69.00"));

            assertThat(payment.amount()).isEqualTo(rm("299.00"));
            assertThat(payment.collected()).isEqualTo(rm("0.00"));
            assertThat(payment.refunded()).isEqualTo(rm("0.00"));
            assertThat(payment.state()).isEqualTo(PaymentState.AUTHORISED);
        }

        @Test
        void editingACardRequestDownwardRefundsNothingBecauseNothingWasCharged() {
            Payment payment = anAuthorisedCardPaymentOf("230.00");

            assertThat(payment.adjustTotal(rm("126.50"))).isEqualTo(rm("-103.50"));

            assertThat(payment.amount()).isEqualTo(rm("126.50"));
            assertThat(payment.refunded()).isEqualTo(rm("0.00"));
            assertThat(payment.collected()).isEqualTo(rm("0.00"));
        }

        @Test
        void anEditedCardIsChargedTheNewTotal() {
            Payment payment = anAuthorisedCardPaymentOf("230.00");
            payment.adjustTotal(rm("299.00"));

            payment.capture(NOW);

            assertThat(payment.collected()).isEqualTo(rm("299.00"));
        }

        @Test
        void anAcceptedPaymentIsNotAdjusted() {
            assertThatThrownBy(() -> aHeldPaymentOf("230.00").adjustTotal(rm("299.00")))
                    .isInstanceOf(PaymentAlreadyAcceptedException.class);
        }

        @Test
        void theNewTotalIsAboveZero() {
            assertThatThrownBy(() -> aHeldFpxPaymentOf("230.00").adjustTotal(rm("0")))
                    .isInstanceOf(InvalidPaymentAmountException.class);
        }
    }
}
