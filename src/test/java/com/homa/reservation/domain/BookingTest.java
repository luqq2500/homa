package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.STANDARD_HOURS;
import static com.homa.reservation.domain.ReservationFixtures.KITCHEN_CLEANING;
import static com.homa.reservation.domain.ReservationFixtures.LAUNDRY_FOLDING;
import static com.homa.reservation.domain.ReservationFixtures.aisyahAddress;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.standardBooking;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.reservation.domain.exception.IncompleteBookingException;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class BookingTest {

    @Test
    @Tag("RES-2")
    @DisplayName("Aisyah picks kitchen cleaning and laundry folding, Saturday 10:00, 4 hours, 2 helpers and her saved address. She can't continue without choosing a job.")
    void bookingNeedsAtLeastOneJob() {
        Booking booking = standardBooking();
        assertThat(booking.jobs().jobCodes()).containsExactlyInAnyOrder(KITCHEN_CLEANING, LAUNDRY_FOLDING);
        assertThat(booking.helpers()).isEqualTo(2);

        assertThatThrownBy(() -> new JobSelection(Set.of())).isInstanceOf(IncompleteBookingException.class);
    }

    @Test
    @Tag("RES-2")
    @DisplayName("A booking needs at least one helper; one is enough.")
    void bookingNeedsAtLeastOneHelper() {
        assertThat(booking(1).helpers()).isEqualTo(1);
        assertThatThrownBy(() -> booking(0)).isInstanceOf(IncompleteBookingException.class);
    }

    @Test
    @Tag("RES-2")
    @DisplayName("A booking needs a home address with its city.")
    void bookingNeedsAHomeAddressWithItsCity() {
        assertThatThrownBy(() -> new ServiceAddress(" ", "SALAK_TINGGI", "Guardhouse"))
                .isInstanceOf(IncompleteBookingException.class);
        assertThatThrownBy(() -> new ServiceAddress("B-12-3", " ", "Guardhouse"))
                .isInstanceOf(IncompleteBookingException.class);
        assertThat(new ServiceAddress("B-12-3", "SALAK_TINGGI", "").city()).isEqualTo("SALAK_TINGGI");
    }

    @Test
    @Tag("RES-2")
    @DisplayName("Every selected job must be one of the provider's jobs.")
    void selectedJobsMustAllBeOffered() {
        JobSelection selection = JobSelection.of(KITCHEN_CLEANING, LAUNDRY_FOLDING);
        assertThat(selection.isOfferedBy(Set.of(KITCHEN_CLEANING, LAUNDRY_FOLDING, "BATHROOM_CLEANING"))).isTrue();
        assertThat(selection.isOfferedBy(Set.of(KITCHEN_CLEANING))).isFalse();
    }

    @Test
    @Tag("RES-2")
    @DisplayName("Moving the start keeps the jobs, hours, helpers and address.")
    void bookingStartingLaterKeepsEverythingElse() {
        Booking moved = standardBooking().startingAt(saturday(11, 0));
        assertThat(moved).isEqualTo(new Booking(AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID,
                JobSelection.of(KITCHEN_CLEANING, LAUNDRY_FOLDING), JobTime.of(saturday(11, 0), STANDARD_HOURS), 2,
                aisyahAddress()));
    }

    private static Booking booking(int helpers) {
        return new Booking(AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, JobSelection.of(KITCHEN_CLEANING),
                JobTime.of(JOB_START, STANDARD_HOURS), helpers, aisyahAddress());
    }
}
