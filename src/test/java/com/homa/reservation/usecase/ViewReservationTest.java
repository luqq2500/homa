package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.ACCEPTANCE_DEADLINE;
import static com.homa.fixtures.HomaFixtures.AISYAH_CITY;
import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.FARID_MEMBER_ID;
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
import static com.homa.reservation.usecase.ReservationTestBed.MEI_CUSTOMER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.friday;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.usecase.dto.AssignRosterRequest;
import com.homa.reservation.usecase.dto.CompleteReservationRequest;
import com.homa.reservation.usecase.dto.ViewReservationRequest;
import com.homa.reservation.usecase.dto.ViewReservationResponse;
import com.homa.reservation.usecase.dto.ViewReservationResponse.RosteredMember;
import com.homa.reservation.usecase.exception.NotReservationPartyException;
import com.homa.reservation.usecase.exception.ReservationNotFoundException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ViewReservationTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    private ViewReservationResponse view(Reservation reservation, String viewerId) {
        return bed.viewReservation.view(new ViewReservationRequest(reservation.id().value(), viewerId));
    }

    @Test
    @Tag("ROS-6")
    @DisplayName("On Friday Aisyah sees Siti, Nurul and Farid, each by name and verified photo.")
    void customerSeesTheRosterByNameAndPhoto() {
        Reservation reserved = bed.reserveStandard();
        bed.time.set(friday(18, 0));
        bed.assignRoster.assign(new AssignRosterRequest(reserved.id().value(), KEMAS_HOME_PROVIDER_ID,
                List.of(SITI_MEMBER_ID, NURUL_MEMBER_ID, FARID_MEMBER_ID)));

        ViewReservationResponse seen = view(reserved, AISYAH_CUSTOMER_ID);

        assertThat(seen).isEqualTo(new ViewReservationResponse(reserved.id().value(), "RESERVED",
                AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, List.of(KITCHEN_CLEANING, LAUNDRY_FOLDING),
                saturday(9, 45), JOB_START, JOB_END, 2, ReservationTestBed.HOME_STREET_ADDRESS, AISYAH_CITY,
                ReservationTestBed.HOME_ARRIVAL_NOTE, Money.myr(180),
                Money.myr(20), Money.myr(200), REQUESTED_AT, ACCEPTANCE_DEADLINE, ACCEPTED_AT, null,
                List.of(new RosteredMember(SITI_MEMBER_ID, "HELPER", "Siti", "photo-siti"),
                        new RosteredMember(NURUL_MEMBER_ID, "HELPER", "Nurul", "photo-nurul"),
                        new RosteredMember(FARID_MEMBER_ID, "SUPERVISOR", "Farid", "photo-farid")),
                List.of()));
        assertThat(view(reserved, KEMAS_HOME_PROVIDER_ID)).isEqualTo(seen);
    }

    @Test
    @Tag("ROS-6")
    @DisplayName("Before Kemas Home rosters anyone, a Requested reservation shows no roster and isn't Reserved or Completed.")
    void requestedReservationHasNoRosterYet() {
        ViewReservationResponse seen = view(bed.requestStandard(), AISYAH_CUSTOMER_ID);

        assertThat(seen.status()).isEqualTo("REQUESTED");
        assertThat(seen.roster()).isEmpty();
        assertThat(seen.reservedAt()).isNull();
        assertThat(seen.completedAt()).isNull();
        assertThat(seen.acceptanceDeadline()).isEqualTo(ACCEPTANCE_DEADLINE);
    }

    @Test
    @Tag("JOB-6")
    @DisplayName("A Completed reservation shows when it was marked and its after-photos.")
    void completedReservationShowsItsAfterPhotos() {
        Reservation reserved = bed.reserveStandard();
        bed.photoStore.upload(CLEAN_KITCHEN_PHOTO);
        bed.time.set(saturday(14, 1));
        bed.completeReservation.complete(new CompleteReservationRequest(reserved.id().value(),
                KEMAS_HOME_PROVIDER_ID, List.of(CLEAN_KITCHEN_PHOTO)));

        ViewReservationResponse seen = view(reserved, AISYAH_CUSTOMER_ID);

        assertThat(seen.status()).isEqualTo("COMPLETED");
        assertThat(seen.completedAt()).isEqualTo(saturday(14, 1));
        assertThat(seen.completionPhotoRefs()).containsExactly(CLEAN_KITCHEN_PHOTO);
    }

    @Test
    @Tag("JOB-1")
    @DisplayName("The team arrives at 9:52 and starts at 10:00.")
    void bothPartiesSeeTheCheckInPeriodInWhichTheRosteredTeamArrives() {
        Reservation reserved = bed.reserveStandard();

        for (String viewer : List.of(AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID)) {
            ViewReservationResponse seen = view(reserved, viewer);

            assertThat(seen.checkInStart()).isEqualTo(saturday(9, 45));
            assertThat(seen.jobStart()).isEqualTo(JOB_START);
            assertThat(Duration.between(seen.checkInStart(), seen.jobStart())).isEqualTo(Duration.ofMinutes(15));
            assertThat(saturday(9, 52)).isAfterOrEqualTo(seen.checkInStart()).isBefore(seen.jobStart());
        }
    }

    @Test
    @Tag("ROS-6")
    @DisplayName("Only Aisyah and Kemas Home see the reservation; Mei can't.")
    void onlyThePartiesSeeTheReservation() {
        Reservation requested = bed.requestStandard();

        assertThatThrownBy(() -> view(requested, MEI_CUSTOMER_ID)).isInstanceOf(NotReservationPartyException.class);
        assertThatThrownBy(() -> bed.viewReservation.view(
                new ViewReservationRequest("reservation-unknown", AISYAH_CUSTOMER_ID)))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}
