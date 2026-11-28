package com.homa.payment.usecase.ie;

import static com.homa.payment.usecase.PaymentUsecaseFixtures.CHECKOUT_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PAID_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVATION;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.aisyahsHeldPayment;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.aisyahsPendingHold;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.requested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.payment.domain.LedgerAccount;
import com.homa.payment.domain.LedgerEntry;
import com.homa.payment.domain.LedgerMovement;
import com.homa.payment.domain.PaymentHold;
import com.homa.payment.domain.ReservationId;
import com.homa.payment.domain.exception.PaymentHoldAlreadyLinkedException;
import com.homa.payment.domain.exception.PaymentNotHeldException;
import com.homa.payment.usecase.FixedTimeSource;
import com.homa.payment.usecase.InMemoryLedgerRepository;
import com.homa.payment.usecase.InMemoryPaymentHoldRepository;
import com.homa.payment.usecase.InMemoryProcessedMessageStore;
import com.homa.payment.usecase.exception.PaymentHoldNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleReservationRequestedIETest {

    private final InMemoryPaymentHoldRepository holds = new InMemoryPaymentHoldRepository();
    private final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();
    private final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    private final FixedTimeSource time = new FixedTimeSource(PAID_AT);
    private final HandleReservationRequestedIE handler =
            new HandleReservationRequestedIE(holds, ledger, processed, time);

    @Test
    @Tag("RES-7")
    @Tag("PAY-1")
    @DisplayName("Aisyah pays RM200 on Monday 20:00. Her reservation is Requested: the payment is held for it, and "
            + "later events about the reservation find it.")
    void requestedReservationIsLinkedToItsHeldPayment() {
        PaymentHold hold = aisyahsHeldPayment();
        holds.given(hold);

        handler.handle(requested("reservation-event-1"));

        assertThat(hold.reservationId()).contains(RESERVATION);
        assertThat(holds.saves()).containsExactly(hold);
        assertThat(holds.findByReservationId(RESERVATION)).contains(hold);
        assertThat(processed.recorded()).containsExactly("reservation-event-1|payment.ReservationRequested");
    }

    @Test
    @Tag("PAY-1")
    @Tag("PAY-4")
    @DisplayName("Aisyah's RM200 is held from Monday 20:00: Homa's bank holds it for her.")
    void heldPaymentIsPostedToTheLedgerWhenTheReservationIsRequested() {
        holds.given(aisyahsHeldPayment());

        handler.handle(requested("reservation-event-1"));

        assertThat(ledger.entries())
                .containsExactly(new LedgerEntry(LedgerMovement.PAYMENT_HELD, Money.myr(200), PAID_AT));
        assertThat(ledger.balanceOf(LedgerAccount.HOMA_BANK)).isEqualTo(Money.myr(200));
        assertThat(ledger.balanceOf(LedgerAccount.CUSTOMER_HELD)).isEqualTo(Money.myr(200));
    }

    @Test
    @Tag("RES-7")
    @DisplayName("A redelivered Requested event links and posts nothing a second time.")
    void redeliveredEventChangesNothing() {
        holds.given(aisyahsHeldPayment());
        handler.handle(requested("reservation-event-1"));

        handler.handle(requested("reservation-event-1"));

        assertThat(holds.saves()).hasSize(1);
        assertThat(ledger.entries()).hasSize(1);
    }

    @Test
    @Tag("RES-7")
    @DisplayName("An event this handler already handled is not applied again.")
    void alreadyHandledEventIsNotApplied() {
        PaymentHold hold = aisyahsHeldPayment();
        holds.given(hold);
        processed.recordFirstHandling("reservation-event-1", HandleReservationRequestedIE.HANDLER);

        handler.handle(requested("reservation-event-1"));

        assertThat(hold.reservationId()).isEmpty();
        assertThat(holds.saves()).isEmpty();
        assertThat(ledger.entries()).isEmpty();
    }

    @Test
    @Tag("RES-7")
    @DisplayName("A Requested event for a checkout Payment never opened is refused, so it is retried and reported.")
    void requestedEventForAnUnknownCheckoutIsRefused() {
        assertThatThrownBy(() -> handler.handle(requested("reservation-event-1")))
                .isInstanceOf(PaymentHoldNotFoundException.class)
                .hasMessageContaining(CHECKOUT_ID);
        assertThat(ledger.entries()).isEmpty();
    }

    @Test
    @Tag("RES-7")
    @DisplayName("A payment still waiting for its confirmation cannot be linked to a Requested reservation.")
    void paymentNotYetHeldCannotBeLinked() {
        holds.given(aisyahsPendingHold());

        assertThatThrownBy(() -> handler.handle(requested("reservation-event-1")))
                .isInstanceOf(PaymentNotHeldException.class);
        assertThat(holds.saves()).isEmpty();
        assertThat(ledger.entries()).isEmpty();
    }

    @Test
    @Tag("RES-7")
    @DisplayName("A payment held for one reservation is never linked to another: the event is refused, not retried "
            + "quietly.")
    void paymentHeldForAnotherReservationIsNotRelinked() {
        PaymentHold hold = aisyahsHeldPayment();
        hold.linkToReservation(new ReservationId("reservation-other"));
        holds.given(hold);

        assertThatThrownBy(() -> handler.handle(requested("reservation-event-1")))
                .isInstanceOf(PaymentHoldAlreadyLinkedException.class)
                .hasMessageContaining("reservation-other");
        assertThat(hold.reservationId()).contains(new ReservationId("reservation-other"));
        assertThat(ledger.entries()).isEmpty();
    }
}
