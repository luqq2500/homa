package com.homa.payment.usecase.ie;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.KEMAS_HOME;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.PROTECTION_WINDOW_END;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVATION;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVATION_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.RESERVED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.reserved;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.ReservationReservedIE;
import com.homa.kernel.Money;
import com.homa.payment.domain.Payout;
import com.homa.payment.domain.PayoutKind;
import com.homa.payment.domain.PayoutStatus;
import com.homa.payment.usecase.InMemoryPayoutRepository;
import com.homa.payment.usecase.InMemoryProcessedMessageStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleReservationReservedIETest {

    private final InMemoryPayoutRepository payouts = new InMemoryPayoutRepository();
    private final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    private final HandleReservationReservedIE handler = new HandleReservationReservedIE(payouts, processed);

    @Test
    @Tag("PAY-2")
    @Tag("RPT-6")
    @DisplayName("Kemas Home accepts on Monday 22:00: its RM200 JOB payout opens, waiting for the job and the "
            + "Protection Window.")
    void reservedReservationOpensItsJobPayout() {
        handler.handle(reserved("reservation-event-1"));

        Payout payout = payouts.findByReservationId(RESERVATION).orElseThrow();
        assertThat(payouts.saves()).containsExactly(payout);
        assertThat(payout.id().value()).isNotBlank();
        assertThat(payout.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(payout.kind()).isEqualTo(PayoutKind.JOB);
        assertThat(payout.gross()).isEqualTo(Money.myr(200));
        assertThat(payout.requestedAt()).isEqualTo(REQUESTED_AT);
        assertThat(payout.protectionWindowEnd()).isEqualTo(PROTECTION_WINDOW_END);
        assertThat(payout.completedAt()).isEmpty();
        assertThat(payout.status()).isEqualTo(PayoutStatus.PENDING);
        assertThat(processed.recorded()).containsExactly("reservation-event-1|payment.ReservationReserved");
    }

    @Test
    @Tag("PAY-2")
    @DisplayName("A redelivered Reserved opens no second payout.")
    void redeliveredEventChangesNothing() {
        handler.handle(reserved("reservation-event-1"));
        Payout first = payouts.findByReservationId(RESERVATION).orElseThrow();

        handler.handle(reserved("reservation-event-1"));

        assertThat(payouts.saves()).containsExactly(first);
        assertThat(payouts.all()).containsExactly(first);
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("A total Payment can't pay opens no payout, and the event is not retried forever.")
    void totalPaymentCannotPayOpensNothing() {
        handler.handle(new ReservationReservedIE("reservation-event-1", RESERVATION_ID, AISYAH_CUSTOMER_ID,
                KEMAS_HOME_PROVIDER_ID, JOB_START, JOB_START, JOB_END, Money.myr(0), PROTECTION_WINDOW_END,
                REQUESTED_AT, RESERVED_AT));

        assertThat(payouts.all()).isEmpty();
        assertThat(processed.recorded()).hasSize(1);
    }
}
