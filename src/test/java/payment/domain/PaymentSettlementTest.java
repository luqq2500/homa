package payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static payment.domain.PaymentBuilders.NOW;
import static payment.domain.PaymentBuilders.aHeldFpxPaymentOf;
import static payment.domain.PaymentBuilders.aHeldPaymentOf;
import static payment.domain.PaymentBuilders.aSettlement;
import static payment.domain.PaymentBuilders.anAuthorisedCardPaymentOf;
import static payment.domain.PaymentBuilders.rm;
import static payment.domain.PaymentBuilders.saturday;

import java.util.List;
import org.junit.jupiter.api.Test;
import payment.domain.PaymentInstruction.Kind;
import payment.domain.exception.PaymentAlreadySettledException;
import payment.domain.exception.PaymentNotAcceptedException;
import payment.domain.exception.SettlementForAnotherReservationException;
import payment.domain.exception.SettlementMismatchException;

/** Feature: Settling. */
class PaymentSettlementTest {

    @Test
    void aSettlementIsFullyAccountedFor() {
        Payment payment = aHeldPaymentOf("230.00");

        List<PaymentInstruction> instructions = payment.settle(aSettlement("18.00", "2.70", "209.30"), NOW);

        assertThat(payment.released()).isEqualTo(rm("20.70"));
        assertThat(payment.refunded()).isEqualTo(rm("209.30"));
        assertThat(payment.isFullyAccounted()).isTrue();
        assertThat(payment.state()).isEqualTo(PaymentState.RELEASED);
        assertThat(instructions).containsExactly(
                new PaymentInstruction(Kind.REFUND_CUSTOMER, rm("209.30"), NOW),
                new PaymentInstruction(Kind.RELEASE_TO_PROVIDER, rm("18.00"), saturday("10:00")),
                new PaymentInstruction(Kind.RELEASE_SERVICE_FEE, rm("2.70"), saturday("10:00")));
    }

    @Test
    void aFullReleaseRefundsNothing() {
        Payment payment = aHeldPaymentOf("230.00");

        List<PaymentInstruction> instructions = payment.settle(aSettlement("200.00", "30.00", "0.00"), NOW);

        assertThat(instructions).extracting(PaymentInstruction::kind)
                .containsExactly(Kind.RELEASE_TO_PROVIDER, Kind.RELEASE_SERVICE_FEE);
        assertThat(payment.state()).isEqualTo(PaymentState.RELEASED);
    }

    @Test
    void aFullRefundReleasesNothing() {
        Payment payment = aHeldPaymentOf("230.00");

        List<PaymentInstruction> instructions = payment.settle(aSettlement("0.00", "0.00", "230.00"), NOW);

        assertThat(instructions).containsExactly(new PaymentInstruction(Kind.REFUND_CUSTOMER, rm("230.00"), NOW));
        assertThat(payment.state()).isEqualTo(PaymentState.REFUNDED);
        assertThat(payment.isFullyAccounted()).isTrue();
    }

    @Test
    void aSettlementThatDoesNotAddUpIsRejected() {
        Payment payment = aHeldPaymentOf("230.00");

        assertThatThrownBy(() -> payment.settle(aSettlement("18.00", "2.70", "208.30"), NOW))
                .isInstanceOf(SettlementMismatchException.class)
                .hasMessageContaining("RM229.00");
        assertThat(payment.state()).isEqualTo(PaymentState.HELD);
        assertThat(payment.isFullyAccounted()).isFalse();
    }

    @Test
    void aPaymentCannotBeSettledTwice() {
        Payment payment = aHeldPaymentOf("230.00");
        payment.settle(aSettlement("18.00", "2.70", "209.30"), NOW);

        assertThatThrownBy(() -> payment.settle(aSettlement("18.00", "2.70", "209.30"), NOW))
                .isInstanceOf(PaymentAlreadySettledException.class);
    }

    @Test
    void onlyAnAcceptedBookingIsSettled() {
        assertThatThrownBy(() -> anAuthorisedCardPaymentOf("230.00").settle(aSettlement("0", "0", "230"), NOW))
                .isInstanceOf(PaymentNotAcceptedException.class);
        assertThatThrownBy(() -> aHeldFpxPaymentOf("230.00").settle(aSettlement("0", "0", "230"), NOW))
                .isInstanceOf(PaymentNotAcceptedException.class);
    }

    @Test
    void aSettlementIsForItsOwnReservation() {
        Settlement another = new Settlement("reservation-2", rm("0"), rm("0"), rm("230"), saturday("10:00"));

        assertThatThrownBy(() -> aHeldPaymentOf("230.00").settle(another, NOW))
                .isInstanceOf(SettlementForAnotherReservationException.class);
    }

    @Test
    void anFpxPaymentEditedDownIsFullyAccountedAfterSettling() {
        Payment payment = aHeldFpxPaymentOf("299.00");
        payment.adjustTotal(rm("230.00"));
        payment.capture(NOW);

        payment.settle(aSettlement("200.00", "30.00", "0.00"), NOW);

        assertThat(payment.collected()).isEqualTo(rm("299.00"));
        assertThat(payment.refunded()).isEqualTo(rm("69.00"));
        assertThat(payment.released()).isEqualTo(rm("230.00"));
        assertThat(payment.isFullyAccounted()).isTrue();
    }

    @Test
    void aHeldPaymentIsNotYetAccountedFor() {
        assertThat(aHeldPaymentOf("230.00").isFullyAccounted()).isFalse();
    }

    @Test
    void anInstructionNamesWhatWhenAndHowMuch() {
        assertThatThrownBy(() -> new PaymentInstruction(null, rm("1"), NOW)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PaymentInstruction(Kind.REFUND_CUSTOMER, null, NOW)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PaymentInstruction(Kind.REFUND_CUSTOMER, rm("1"), null)).isInstanceOf(NullPointerException.class);
    }
}
