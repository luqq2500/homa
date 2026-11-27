package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.KITCHEN_CLEANING;
import static com.homa.reservation.usecase.ReservationTestBed.MEI_CUSTOMER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.friday;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.fixtures.HomaFixtures;
import com.homa.kernel.Money;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.usecase.dto.AssignRosterRequest;
import com.homa.reservation.usecase.dto.ListMyReservationsRequest;
import com.homa.reservation.usecase.dto.ListMyReservationsResponse;
import com.homa.reservation.usecase.dto.ListMyReservationsResponse.ReservationSummary;
import com.homa.reservation.usecase.dto.ListMyReservationsResponse.RosteredMember;
import com.homa.reservation.usecase.dto.StartReservationCheckoutRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ListMyReservationsTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    private List<ReservationSummary> listFor(String partyId) {
        return bed.listMyReservations.list(new ListMyReservationsRequest(partyId)).reservations();
    }

    /** Aisyah also books Kemas Home on Friday 10:00–12:00, with no discount: 25 × 2 × 1 + 20 = RM70. */
    private Reservation bookFriday() {
        bed.time.set(HomaFixtures.REQUESTED_AT);
        String checkoutId = bed.startCheckout.start(new StartReservationCheckoutRequest(AISYAH_CUSTOMER_ID,
                KEMAS_HOME_PROVIDER_ID, List.of(KITCHEN_CLEANING), friday(10, 0), 120, 1)).checkoutId();
        bed.paymentHeld.handle(bed.paymentHeld(checkoutId, HomaFixtures.REQUESTED_AT));
        return bed.reservations.findByParty(AISYAH_CUSTOMER_ID).stream()
                .filter(reservation -> reservation.checkoutId().value().equals(checkoutId)).findFirst().orElseThrow();
    }

    @Test
    @Tag("ROS-6")
    @DisplayName("Aisyah and Kemas Home each list Friday's and Saturday's reservations, earliest first, with Saturday's roster.")
    void partiesListTheirReservationsWithTheRoster() {
        Reservation saturdayJob = bed.reserveStandard();
        Reservation fridayJob = bookFriday();
        bed.time.set(friday(18, 0));
        bed.assignRoster.assign(new AssignRosterRequest(saturdayJob.id().value(), KEMAS_HOME_PROVIDER_ID,
                List.of(SITI_MEMBER_ID, NURUL_MEMBER_ID)));

        List<ReservationSummary> aisyahs = listFor(AISYAH_CUSTOMER_ID);

        assertThat(aisyahs).containsExactly(
                new ReservationSummary(fridayJob.id().value(), "REQUESTED", AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID,
                        friday(10, 0), friday(12, 0), 1, Money.myr(70), List.of()),
                new ReservationSummary(saturdayJob.id().value(), "RESERVED", AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID,
                        JOB_START, JOB_END, 2, Money.myr(200), List.of(
                                new RosteredMember(SITI_MEMBER_ID, "HELPER", "Siti", "photo-siti"),
                                new RosteredMember(NURUL_MEMBER_ID, "HELPER", "Nurul", "photo-nurul"))));
        assertThat(listFor(KEMAS_HOME_PROVIDER_ID)).isEqualTo(aisyahs);
    }

    @Test
    @Tag("ROS-6")
    @DisplayName("Mei has booked nothing, so her list is empty.")
    void partyWithoutReservationsListsNone() {
        bed.requestStandard();

        assertThat(bed.listMyReservations.list(new ListMyReservationsRequest(MEI_CUSTOMER_ID)))
                .isEqualTo(new ListMyReservationsResponse(List.of()));
    }
}
