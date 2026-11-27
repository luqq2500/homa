package com.homa.reservation.usecase.ie;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.PaymentFailedIE;
import com.homa.ie.events.ReservationRequestedIE;
import com.homa.reservation.usecase.ReservationTestBed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandlePaymentFailedIETest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    @Test
    @Tag("RES-11")
    @DisplayName("Aisyah's card is declined: nothing was held, so nothing is released, and her retry is Requested.")
    void failedPaymentHoldsNothingAndTheCustomerCanRetry() {
        String checkoutId = bed.startStandardCheckout(AISYAH_CUSTOMER_ID);
        bed.publisher.clear();
        PaymentFailedIE declined = bed.paymentFailed(checkoutId);

        bed.paymentFailed.handle(declined);

        assertThat(bed.processed.handled(declined.eventId(), HandlePaymentFailedIE.HANDLER)).isTrue();
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).isEmpty();
        assertThat(bed.reservations.count()).isZero();
        assertThat(bed.publisher.published()).isEmpty();

        bed.time.set(REQUESTED_AT);
        bed.paymentHeld.handle(bed.paymentHeld(checkoutId, REQUESTED_AT));
        assertThat(bed.publisher.single(ReservationRequestedIE.class).checkoutId()).isEqualTo(checkoutId);
    }

    @Test
    @Tag("RES-11")
    @DisplayName("A redelivered failure changes nothing and publishes nothing.")
    void redeliveredFailureIsIgnored() {
        String checkoutId = bed.startStandardCheckout(AISYAH_CUSTOMER_ID);
        bed.publisher.clear();
        PaymentFailedIE declined = bed.paymentFailed(checkoutId);
        bed.paymentFailed.handle(declined);

        bed.paymentFailed.handle(declined);

        assertThat(bed.processed.recordFirstHandling(declined.eventId(), HandlePaymentFailedIE.HANDLER)).isFalse();
        assertThat(HandlePaymentFailedIE.HANDLER).isEqualTo("reservation.PaymentFailed");
        assertThat(bed.checkouts.all()).hasSize(1);
        assertThat(bed.publisher.published()).isEmpty();
    }
}
