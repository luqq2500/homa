package com.homa.payment.usecase.ie;

import static com.homa.payment.usecase.PaymentUsecaseFixtures.COMPLETED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PROTECTION_WINDOW_END;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVATION_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.completed;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.kemasHomesCompletedPayout;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.kemasHomesPayoutAtReserved;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.payment.domain.Payout;
import com.homa.payment.usecase.InMemoryPayoutRepository;
import com.homa.payment.usecase.InMemoryProcessedMessageStore;
import com.homa.payment.usecase.exception.PayoutNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleReservationCompletedIETest {

    private final InMemoryPayoutRepository payouts = new InMemoryPayoutRepository();
    private final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    private final HandleReservationCompletedIE handler = new HandleReservationCompletedIE(payouts, processed);

    @Test
    @Tag("PAY-2")
    @Tag("JOB-3")
    @DisplayName("Kemas Home marks the job Completed on Saturday 14:00: the payout is releasable from Sunday 14:00.")
    void completedJobMakesThePayoutOwed() {
        Payout payout = kemasHomesPayoutAtReserved();
        payouts.given(payout);

        handler.handle(completed("reservation-event-2"));

        assertThat(payout.completedAt()).contains(COMPLETED_AT);
        assertThat(payout.releasableAt(PROTECTION_WINDOW_END)).isTrue();
        assertThat(payouts.saves()).containsExactly(payout);
        assertThat(processed.recorded()).containsExactly("reservation-event-2|payment.ReservationCompleted");
    }

    @Test
    @Tag("PAY-2")
    @DisplayName("A redelivered Completed changes nothing.")
    void redeliveredEventChangesNothing() {
        payouts.given(kemasHomesPayoutAtReserved());
        handler.handle(completed("reservation-event-2"));

        handler.handle(completed("reservation-event-2"));

        assertThat(payouts.saves()).hasSize(1);
    }

    @Test
    @Tag("PAY-2")
    @DisplayName("An event this handler already handled is not applied again, whatever state the payout is in.")
    void alreadyHandledEventIsNotApplied() {
        Payout payout = kemasHomesPayoutAtReserved();
        payouts.given(payout);
        processed.recordFirstHandling("reservation-event-2", HandleReservationCompletedIE.HANDLER);

        handler.handle(completed("reservation-event-2"));

        assertThat(payout.completedAt()).isEmpty();
        assertThat(payouts.saves()).isEmpty();
    }

    @Test
    @Tag("JOB-3")
    @DisplayName("A payout whose job is already marked Completed changes nothing, and the event is not retried "
            + "forever.")
    void payoutNoLongerWaitingForItsJobChangesNothing() {
        Payout payout = kemasHomesCompletedPayout();
        payouts.given(payout);

        handler.handle(completed("reservation-event-9"));

        assertThat(payout.completedAt()).contains(COMPLETED_AT);
        assertThat(payouts.saves()).isEmpty();
        assertThat(processed.recorded()).hasSize(1);
    }

    @Test
    @DisplayName("A Completed for a reservation Payment never saw Reserved is refused, so it is retried and "
            + "reported.")
    void completionWithoutAPayoutIsRefused() {
        assertThatThrownBy(() -> handler.handle(completed("reservation-event-2")))
                .isInstanceOf(PayoutNotFoundException.class)
                .hasMessageContaining(RESERVATION_ID);
    }
}
