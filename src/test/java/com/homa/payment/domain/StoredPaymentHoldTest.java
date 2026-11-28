package com.homa.payment.domain;

import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.payment.domain.PaymentFixtures.AISYAH;
import static com.homa.payment.domain.PaymentFixtures.AISYAHS_CARD_RETRY;
import static com.homa.payment.domain.PaymentFixtures.AISYAHS_HOLD;
import static com.homa.payment.domain.PaymentFixtures.AISYAHS_ONLINE_BANKING;
import static com.homa.payment.domain.PaymentFixtures.CARD_RETRY_CONFIRMED_AT;
import static com.homa.payment.domain.PaymentFixtures.PAID_AT;
import static com.homa.payment.domain.PaymentFixtures.STANDARD_CHECKOUT;
import static com.homa.payment.domain.PaymentFixtures.STANDARD_RESERVATION;
import static com.homa.payment.domain.PaymentFixtures.aisyahsCheckoutAwaitingPayment;
import static com.homa.payment.domain.PaymentFixtures.aisyahsHeldPayment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.payment.domain.exception.AmountNotPayableException;
import com.homa.payment.domain.exception.PaymentHoldAlreadyLinkedException;
import com.homa.payment.domain.exception.PaymentNotHeldException;
import com.homa.payment.domain.exception.RefundExceedsHeldAmountException;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class StoredPaymentHoldTest {

    private static final Money NOTHING = Money.myr(0);

    private static PaymentHold restored(PaymentHold stored) {
        return stored(stored.status(), stored.confirmedAttempts(), stored.countedAttempt().orElse(null),
                stored.heldSince().orElse(null), stored.held(), stored.reservationId().orElse(null));
    }

    private static PaymentHold stored(PaymentHoldStatus status, Set<PaymentAttemptId> confirmed,
            PaymentAttemptId counted, Instant heldSince, Money held) {
        return stored(status, confirmed, counted, heldSince, held, null);
    }

    private static PaymentHold stored(PaymentHoldStatus status, Set<PaymentAttemptId> confirmed,
            PaymentAttemptId counted, Instant heldSince, Money held, ReservationId reservationId) {
        return PaymentHold.restore(AISYAHS_HOLD, STANDARD_CHECKOUT, AISYAH, TOTAL_RESERVATION_FEE, status, confirmed,
                counted, heldSince, held, reservationId);
    }

    private static void assertRefused(PaymentHoldStatus status, Set<PaymentAttemptId> confirmed,
            PaymentAttemptId counted, Instant heldSince, Money held) {
        assertThatThrownBy(() -> stored(status, confirmed, counted, heldSince, held))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("A stored payment hold needs ");
    }

    @Test
    @Tag("RES-12")
    @DisplayName("Persistence: Aisyah's held RM200 comes back with every confirmed attempt, so a repeated confirmation changes nothing and her card retry is refunded in full.")
    void aStoredHeldPaymentKeepsItsConfirmedAttempts() {
        PaymentHold hold = aisyahsHeldPayment();
        hold.recordConfirmation(AISYAHS_CARD_RETRY, CARD_RETRY_CONFIRMED_AT);
        hold.refund(Money.myr(45));

        PaymentHold stored = restored(hold);

        assertThat(stored).usingRecursiveComparison().isEqualTo(hold);
        assertThat(stored.confirmedAttempts()).containsExactlyInAnyOrder(AISYAHS_ONLINE_BANKING, AISYAHS_CARD_RETRY);
        assertThat(stored.recordConfirmation(AISYAHS_CARD_RETRY, CARD_RETRY_CONFIRMED_AT))
                .isEqualTo(new PaymentConfirmation.AlreadyRecorded(AISYAHS_CARD_RETRY));
        PaymentAttemptId another = new PaymentAttemptId("attempt-aisyah-another");
        assertThat(stored.recordConfirmation(another, CARD_RETRY_CONFIRMED_AT))
                .isInstanceOf(PaymentConfirmation.RefundedInFull.class);
        assertThatThrownBy(() -> stored.refund(Money.myr(156))).isInstanceOf(RefundExceedsHeldAmountException.class);
        assertThat(stored.refundRejectedCheckout().amount()).isEqualTo(Money.myr(155));
        assertThat(stored.status()).isEqualTo(PaymentHoldStatus.REFUNDED);
    }

    @Test
    @Tag("PAY-1")
    @DisplayName("Persistence: a stored checkout awaiting payment comes back pending, and its first confirmed payment is held.")
    void aStoredPendingPaymentAwaitsItsFirstConfirmation() {
        PaymentHold awaiting = aisyahsCheckoutAwaitingPayment();

        PaymentHold stored = restored(awaiting);

        assertThat(stored).usingRecursiveComparison().isEqualTo(awaiting);
        assertThat(stored.startAttempt()).isNotNull();
        assertThat(stored.recordConfirmation(AISYAHS_ONLINE_BANKING, PAID_AT))
                .isInstanceOf(PaymentConfirmation.Counted.class);
        assertThat(stored.held()).isEqualTo(TOTAL_RESERVATION_FEE);
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("Persistence: a stored payment refunded in full has nothing left to refund.")
    void aStoredRefundedPaymentHasNothingLeft() {
        PaymentHold refunded = aisyahsHeldPayment();
        refunded.refundRejectedCheckout();

        PaymentHold stored = restored(refunded);

        assertThat(stored).usingRecursiveComparison().isEqualTo(refunded);
        assertThatThrownBy(() -> stored.refund(Money.myr(1))).isInstanceOf(PaymentNotHeldException.class);
    }

    @Test
    @Tag("RES-12")
    @DisplayName("Persistence: a stored payment hold has a counted attempt, with its time, exactly once a payment is confirmed, and it is one of the confirmed attempts.")
    void aStoredCountedAttemptMustFitTheStatus() {
        Set<PaymentAttemptId> banking = Set.of(AISYAHS_ONLINE_BANKING);

        assertRefused(PaymentHoldStatus.PENDING, banking, AISYAHS_ONLINE_BANKING, PAID_AT, NOTHING);
        assertRefused(PaymentHoldStatus.HELD, Set.of(), null, null, TOTAL_RESERVATION_FEE);
        assertRefused(PaymentHoldStatus.HELD, banking, AISYAHS_ONLINE_BANKING, null, TOTAL_RESERVATION_FEE);
        assertRefused(PaymentHoldStatus.PENDING, Set.of(), null, PAID_AT, NOTHING);
        assertRefused(PaymentHoldStatus.HELD, Set.of(AISYAHS_CARD_RETRY), AISYAHS_ONLINE_BANKING, PAID_AT,
                TOTAL_RESERVATION_FEE);
        assertRefused(PaymentHoldStatus.PENDING, banking, null, null, NOTHING);
    }

    @Test
    @Tag("PAY-4")
    @DisplayName("Persistence: a stored held payment holds something, never more than the total; a pending or refunded one holds nothing.")
    void aStoredAmountHeldMustFitTheStatus() {
        Set<PaymentAttemptId> banking = Set.of(AISYAHS_ONLINE_BANKING);

        assertRefused(PaymentHoldStatus.HELD, banking, AISYAHS_ONLINE_BANKING, PAID_AT, NOTHING);
        assertRefused(PaymentHoldStatus.HELD, banking, AISYAHS_ONLINE_BANKING, PAID_AT, Money.myr("200.01"));
        assertRefused(PaymentHoldStatus.REFUNDED, banking, AISYAHS_ONLINE_BANKING, PAID_AT, Money.myr(1));
        assertRefused(PaymentHoldStatus.PENDING, Set.of(), null, null, Money.myr(1));
        assertThat(stored(PaymentHoldStatus.HELD, banking, AISYAHS_ONLINE_BANKING, PAID_AT, Money.myr("0.01"))
                .held()).isEqualTo(Money.myr("0.01"));
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("Persistence: a stored payment hold needs its status, attempts, amount held and a payable total.")
    void aStoredPaymentHoldMissingAPartIsRefused() {
        assertThatThrownBy(() -> stored(null, Set.of(), null, null, NOTHING)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> stored(PaymentHoldStatus.PENDING, null, null, null, NOTHING))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> stored(PaymentHoldStatus.PENDING, Set.of(), null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PaymentHold.restore(AISYAHS_HOLD, STANDARD_CHECKOUT, AISYAH, NOTHING,
                PaymentHoldStatus.PENDING, Set.of(), null, null, NOTHING, null))
                .isInstanceOf(AmountNotPayableException.class);
    }

    @Test
    @Tag("RES-7")
    @DisplayName("Persistence: Aisyah's held RM200 comes back linked to her Requested reservation, so a withdrawal "
            + "finds it and a second reservation cannot take it.")
    void aStoredPaymentKeepsTheReservationItIsLinkedTo() {
        PaymentHold hold = aisyahsHeldPayment();
        hold.linkToReservation(STANDARD_RESERVATION);

        PaymentHold stored = restored(hold);

        assertThat(stored).usingRecursiveComparison().isEqualTo(hold);
        assertThat(stored.reservationId()).contains(STANDARD_RESERVATION);
        assertThatThrownBy(() -> stored.linkToReservation(new ReservationId("reservation-other")))
                .isInstanceOf(PaymentHoldAlreadyLinkedException.class);
        assertThat(restored(aisyahsHeldPayment()).reservationId()).isEmpty();
    }

    @Test
    @Tag("RES-7")
    @DisplayName("Persistence: a stored payment hold is linked to a reservation only once its payment is confirmed.")
    void aStoredReservationLinkNeedsAConfirmedPayment() {
        assertRefusedWithReservation(PaymentHoldStatus.PENDING, Set.of(), null, null, NOTHING);
        assertThat(stored(PaymentHoldStatus.REFUNDED, Set.of(AISYAHS_ONLINE_BANKING), AISYAHS_ONLINE_BANKING, PAID_AT,
                NOTHING, STANDARD_RESERVATION).reservationId()).contains(STANDARD_RESERVATION);
    }

    private static void assertRefusedWithReservation(PaymentHoldStatus status, Set<PaymentAttemptId> confirmed,
            PaymentAttemptId counted, Instant heldSince, Money held) {
        assertThatThrownBy(() -> stored(status, confirmed, counted, heldSince, held, STANDARD_RESERVATION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("A stored payment hold needs ");
    }
}
