package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.reservation.usecase.ReservationTestBed.MEI_CUSTOMER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.friday;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static com.homa.reservation.usecase.ReservationTestBed.standardCheckout;
import static com.homa.reservation.usecase.ReservationTestBed.sunday;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.ReservationCancelledIE;
import com.homa.ie.events.ReservationRequestedIE;
import com.homa.kernel.Money;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.usecase.dto.CustomerCancelReservationRequest;
import com.homa.reservation.usecase.dto.PreviewCustomerCancellationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Flow B, a late customer cancellation (Friday 22:00), as far as Reservation takes part: the preview shows the fee,
 * the cancellation settles it, and the event hands Payment every line to execute.
 */
class FlowBLateCancellationTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    @Test
    @Tag("CCX-6")
    @DisplayName("Aisyah cancels on Friday 22:00, pays RM45 and gets RM155 back.")
    void aLateCancellationIsPreviewedThenSettledAndTheTimeOpensForSomeoneElse() {
        Reservation reserved = bed.reserveStandard();
        String reservationId = reserved.id().value();
        bed.publisher.clear();

        // 1. The preview shows a RM45 fee: the Standard cut-off was Friday 21:45 and there is no grace period.
        bed.time.set(friday(22, 0));
        var previewed = bed.previewCancellation.preview(
                new PreviewCustomerCancellationRequest(reservationId, AISYAH_CUSTOMER_ID));
        assertThat(previewed.customerFee()).isEqualTo(Money.myr(45));
        assertThat(previewed.customerRefund()).isEqualTo(Money.myr(155));
        assertThat(bed.reload(reserved).status()).isEqualTo(ReservationStatus.RESERVED);

        // 2. She confirms: fee 45, refund 155, and both schedule blocks are released.
        var cancelled = bed.customerCancel.cancel(new CustomerCancelReservationRequest(reservationId,
                AISYAH_CUSTOMER_ID));
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).isEmpty();
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).isEmpty();

        // 3. Payment gets one event: refund RM155 now, and a FEE payout of RM45 held until Sunday 14:00 (PAY-6, PAY-7).
        assertThat(bed.publisher.published()).containsExactly(new ReservationCancelledIE("reservation-event-4",
                reservationId, AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, "CUSTOMER_CANCELLED", friday(22, 0),
                REQUESTED_AT, saturday(9, 45), sunday(14, 0), "settlement-" + reservationId, Money.myr(45),
                Money.myr(155), Money.myr(0), Money.myr(0), Money.myr(45), friday(22, 0)));

        // The time is free again: Mei's payment a minute later makes her request.
        bed.time.set(friday(22, 1));
        String meiCheckout = bed.startCheckout.start(standardCheckout(MEI_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID))
                .checkoutId();
        bed.paymentHeld.handle(bed.paymentHeld(meiCheckout, friday(22, 2)));
        assertThat(bed.publisher.published().get(bed.publisher.published().size() - 1))
                .isInstanceOf(ReservationRequestedIE.class);
    }
}
