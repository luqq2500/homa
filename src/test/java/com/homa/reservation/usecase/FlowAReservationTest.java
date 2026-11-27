package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.ACCEPTANCE_DEADLINE;
import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.ACCEPTED_AT;
import static com.homa.reservation.usecase.ReservationTestBed.CLEAN_KITCHEN_PHOTO;
import static com.homa.reservation.usecase.ReservationTestBed.KITCHEN_CLEANING;
import static com.homa.reservation.usecase.ReservationTestBed.LAUNDRY_FOLDING;
import static com.homa.reservation.usecase.ReservationTestBed.TAPPED_PAY_AT;
import static com.homa.reservation.usecase.ReservationTestBed.friday;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static com.homa.reservation.usecase.ReservationTestBed.standardCheckout;
import static com.homa.reservation.usecase.ReservationTestBed.sunday;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.ReservationCheckoutStartedIE;
import com.homa.ie.events.ReservationCompletedIE;
import com.homa.ie.events.ReservationRequestedIE;
import com.homa.ie.events.ReservationReservedIE;
import com.homa.ie.events.RosterChangedIE;
import com.homa.kernel.Money;
import com.homa.reservation.usecase.dto.AcceptReservationRequest;
import com.homa.reservation.usecase.dto.AssignRosterRequest;
import com.homa.reservation.usecase.dto.CompleteReservationRequest;
import com.homa.reservation.usecase.dto.FindAvailableStartTimesRequest;
import com.homa.reservation.usecase.dto.QuoteReservationRequest;
import com.homa.reservation.usecase.dto.RemindUncompletedJobsRequest;
import com.homa.reservation.usecase.dto.ViewReservationRequest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Flow A, the happy path, as far as Reservation takes part: checkout, payment held, accept, roster, complete. */
class FlowAReservationTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    @Test
    @Tag("RES-7")
    @Tag("ACC-2")
    @Tag("ROS-1")
    @Tag("JOB-3")
    @DisplayName("Aisyah books Kemas Home for Saturday; it accepts on Monday 22:00, rosters Siti and Nurul on Friday and marks the job Completed at 14:00.")
    void happyPathFromCheckoutToCompleted() {
        // 1. Aisyah finds Saturday 10:00 free and is quoted RM200.
        bed.time.set(TAPPED_PAY_AT);
        assertThat(bed.findAvailableStartTimes.find(new FindAvailableStartTimesRequest(AISYAH_CUSTOMER_ID,
                KEMAS_HOME_PROVIDER_ID, LocalDate.of(2026, 12, 5), List.of(KITCHEN_CLEANING, LAUNDRY_FOLDING), 240,
                2)).startTimes()).contains(JOB_START);
        assertThat(bed.quoteReservation.quote(new QuoteReservationRequest(AISYAH_CUSTOMER_ID,
                KEMAS_HOME_PROVIDER_ID, 240, 2)).total()).isEqualTo(Money.myr(200));

        // 2. She starts paying; nothing is held.
        String checkoutId = bed.startCheckout.start(standardCheckout(AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID))
                .checkoutId();
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).isEmpty();

        // 3-4. Payment confirms at Monday 20:00: Requested, deadline Tuesday 20:00, both schedules blocked.
        bed.time.set(REQUESTED_AT);
        bed.paymentHeld.handle(bed.paymentHeld(checkoutId, REQUESTED_AT));
        String reservationId = bed.reservations.single().id().value();
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).hasSize(1);
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).hasSize(1);

        // 5. Kemas Home accepts at Monday 22:00.
        bed.time.set(ACCEPTED_AT);
        bed.acceptReservation.accept(new AcceptReservationRequest(reservationId, KEMAS_HOME_PROVIDER_ID));

        // 6. On Friday it rosters Siti and Nurul, and Aisyah sees them.
        bed.time.set(friday(18, 0));
        bed.assignRoster.assign(new AssignRosterRequest(reservationId, KEMAS_HOME_PROVIDER_ID,
                List.of(SITI_MEMBER_ID, NURUL_MEMBER_ID)));
        assertThat(bed.viewReservation.view(new ViewReservationRequest(reservationId, AISYAH_CUSTOMER_ID)).roster())
                .extracting(member -> member.name() + " " + member.photoRef())
                .containsExactly("Siti photo-siti", "Nurul photo-nurul");

        // 8. At 14:00 the job isn't marked yet, so Kemas Home is reminded.
        bed.time.set(JOB_END);
        assertThat(bed.remindUncompletedJobs.remind(new RemindUncompletedJobsRequest()).remindersSent())
                .isEqualTo(1);

        // 9. Siti uploads the kitchen photo and marks the job Completed; both calendars open.
        bed.photoStore.upload(CLEAN_KITCHEN_PHOTO);
        bed.time.set(saturday(14, 1));
        bed.completeReservation.complete(new CompleteReservationRequest(reservationId, KEMAS_HOME_PROVIDER_ID,
                List.of(CLEAN_KITCHEN_PHOTO)));
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).isEmpty();
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).isEmpty();
        bed.time.set(saturday(16, 0));
        assertThat(bed.remindUncompletedJobs.remind(new RemindUncompletedJobsRequest()).remindersSent()).isZero();

        assertThat(bed.publisher.published()).containsExactly(
                new ReservationCheckoutStartedIE("reservation-event-1", checkoutId, AISYAH_CUSTOMER_ID,
                        KEMAS_HOME_PROVIDER_ID, Money.myr(200), TAPPED_PAY_AT),
                new ReservationRequestedIE("reservation-event-2", reservationId, checkoutId, AISYAH_CUSTOMER_ID,
                        KEMAS_HOME_PROVIDER_ID, saturday(9, 45), JOB_START, JOB_END, Money.myr(200),
                        ACCEPTANCE_DEADLINE, sunday(14, 0), REQUESTED_AT, REQUESTED_AT),
                new ReservationReservedIE("reservation-event-3", reservationId, AISYAH_CUSTOMER_ID,
                        KEMAS_HOME_PROVIDER_ID, saturday(9, 45), JOB_START, JOB_END, Money.myr(200), sunday(14, 0),
                        REQUESTED_AT, ACCEPTED_AT),
                new RosterChangedIE("reservation-event-4", reservationId, List.of(SITI_MEMBER_ID, NURUL_MEMBER_ID),
                        friday(18, 0)),
                new ReservationCompletedIE("reservation-event-5", reservationId, AISYAH_CUSTOMER_ID,
                        KEMAS_HOME_PROVIDER_ID, Money.myr(200), sunday(14, 0), saturday(14, 1),
                        List.of(CLEAN_KITCHEN_PHOTO), saturday(14, 1)));
    }
}
