package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.STANDARD_HOURS;
import static com.homa.reservation.domain.ReservationFixtures.BOOKING_STEP;
import static com.homa.reservation.domain.ReservationFixtures.aisyahBooking;
import static com.homa.reservation.domain.ReservationFixtures.assertRefused;
import static com.homa.reservation.domain.ReservationFixtures.kemasHome;
import static com.homa.reservation.domain.ReservationFixtures.kemasHomeProvider;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.standardBooking;
import static com.homa.reservation.domain.ReservationFixtures.sunday;
import static com.homa.reservation.domain.ReservationFixtures.tuesday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.homa.fixtures.HomaFixtures;
import com.homa.kernel.TimeInterval;
import com.homa.reservation.domain.replica.BookableProvider;
import com.homa.reservation.domain.replica.BookingBounds;
import com.homa.reservation.domain.replica.DailyHours;
import com.homa.reservation.domain.replica.WorkingHours;
import com.homa.reservation.domain.terms.AcceptancePolicy;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class BookableProviderTest {

    private static final ReservationValues.Version VALUES = ReservationValues.current();

    private final BookableProvider kemasHome = kemasHome();

    @Test
    @Tag("RES-1")
    @DisplayName("Aisyah lives in Salak Tinggi, so she can reserve Kemas Home. Her sister in Shah Alam can't, because Kemas Home doesn't cover Shah Alam.")
    void providerAdmitsOnlyCustomersInACityItCovers() {
        Booking sisterInShahAlam = new Booking("customer-sister", KEMAS_HOME_PROVIDER_ID,
                JobSelection.of(ReservationFixtures.KITCHEN_CLEANING), JobTime.of(JOB_START, STANDARD_HOURS), 2,
                new ServiceAddress("12, Jalan Shah Alam", "SHAH_ALAM", "Ring twice"));

        assertThat(kemasHome.admits(standardBooking(), REQUESTED_AT)).isTrue();
        assertThat(kemasHome.refusalOf(standardBooking(), REQUESTED_AT)).isEmpty();
        assertThat(kemasHome.refusalOf(sisterInShahAlam, REQUESTED_AT)).contains(BookingRefusal.CITY_NOT_COVERED);
    }

    @Test
    @Tag("RES-2")
    @DisplayName("Aisyah can only choose among the provider's jobs.")
    void providerRefusesAJobItDoesNotOffer() {
        Booking windows = new Booking(AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, JobSelection.of("WINDOW_CLEANING"),
                JobTime.of(JOB_START, STANDARD_HOURS), 2, ReservationFixtures.aisyahAddress());

        assertThat(kemasHome.refusalOf(windows, REQUESTED_AT)).contains(BookingRefusal.JOB_NOT_OFFERED);
    }

    @Test
    @Tag("RES-3")
    @DisplayName("Kemas Home works 8:00–18:00, so a job can start at 8:00 (team arrives from 7:45) but can't run 17:00–19:00.")
    void jobTimeMustFallWithinWorkingHours() {
        assertThat(kemasHome.admits(booking(saturday(8, 0), 2), REQUESTED_AT)).isTrue();
        assertThat(kemasHome.refusalOf(booking(saturday(17, 0), 2), REQUESTED_AT))
                .contains(BookingRefusal.OUTSIDE_WORKING_HOURS);
    }

    @Test
    @Tag("RES-3")
    @DisplayName("A job may end exactly at closing, but not start before opening, run on a day off, or run past midnight.")
    void jobTimeIsRefusedJustOutsideWorkingHours() {
        assertThat(kemasHome.admits(booking(saturday(14, 0), 4), REQUESTED_AT)).isTrue();
        assertThat(kemasHome.refusalOf(booking(saturday(7, 45), 2), REQUESTED_AT))
                .contains(BookingRefusal.OUTSIDE_WORKING_HOURS);
        assertThat(kemasHome.refusalOf(booking(sunday(10, 0), 2), REQUESTED_AT))
                .contains(BookingRefusal.OUTSIDE_WORKING_HOURS);

        BookableProvider lateHours = kemasHomeProvider().workingHours(new WorkingHours(
                Map.of(DayOfWeek.SATURDAY, new DailyHours(LocalTime.of(8, 0), LocalTime.of(23, 45))))).build();
        assertThat(lateHours.admits(booking(saturday(21, 0), 2), REQUESTED_AT)).isTrue();
        assertThat(lateHours.refusalOf(booking(saturday(23, 0), 2), REQUESTED_AT))
                .contains(BookingRefusal.OUTSIDE_WORKING_HOURS);
    }

    @Test
    @Tag("RES-3")
    @DisplayName("A private appointment on Tuesday 14:00–16:00 makes that slot unavailable on Homa, including the job's buffer.")
    void externalBusyTimeMakesTheProviderUnavailable() {
        BookableProvider withAppointment = kemasHomeProvider()
                .busy(new TimeInterval(tuesday(14, 0), tuesday(16, 0))).build();

        assertThat(withAppointment.refusalOf(booking(tuesday(14, 0), 2), REQUESTED_AT))
                .contains(BookingRefusal.PROVIDER_BUSY);
        assertThat(withAppointment.refusalOf(booking(tuesday(12, 0), 2), REQUESTED_AT))
                .contains(BookingRefusal.PROVIDER_BUSY);
        assertThat(withAppointment.admits(booking(tuesday(11, 30), 2), REQUESTED_AT)).isTrue();
    }

    @Test
    @Tag("RES-4")
    @DisplayName("It's Saturday 7:00 and Kemas Home's notice is 2 hours. The earliest Check-in Period can start at 9:00, so the earliest job start Aisyah can pick is 9:15.")
    void noticeMustBeAtLeastTheProvidersMinimumNotice() {
        assertThat(kemasHome.admits(booking(saturday(9, 15), 4), saturday(7, 0))).isTrue();
        assertThat(kemasHome.refusalOf(booking(saturday(9, 0), 4), saturday(7, 0)))
                .contains(BookingRefusal.NOTICE_TOO_SHORT);
    }

    @Test
    @Tag("RES-4")
    @DisplayName("At 7:01 the 9:15 start is one minute short of the 2-hour notice.")
    void noticeOneMinuteShortOfTheMinimumIsRefused() {
        assertThat(kemasHome.refusalOf(booking(saturday(9, 15), 4), saturday(7, 1)))
                .contains(BookingRefusal.NOTICE_TOO_SHORT);
    }

    @Test
    @Tag("RES-5")
    @DisplayName("With Kemas Home (minimum 2 hours, maximum 6 hours) Aisyah starts at 2h and can't book 1h 45m, 2h 10m or 6h 15m.")
    void durationMustFallWithinTheProvidersHourBounds() {
        assertThat(kemasHome.refusalOf(booking(saturday(10, 0), Duration.ofMinutes(105), 2), REQUESTED_AT))
                .contains(BookingRefusal.HOURS_OUT_OF_BOUNDS);
        assertThat(kemasHome.admits(booking(saturday(10, 0), Duration.ofHours(2), 2), REQUESTED_AT)).isTrue();
        assertThat(kemasHome.refusalOf(booking(saturday(10, 0), Duration.ofMinutes(130), 2), REQUESTED_AT))
                .contains(BookingRefusal.HOURS_OUT_OF_BOUNDS);
        assertThat(kemasHome.admits(booking(saturday(10, 0), Duration.ofHours(6), 2), REQUESTED_AT)).isTrue();
        assertThat(kemasHome.refusalOf(booking(saturday(10, 0), Duration.ofMinutes(375), 2), REQUESTED_AT))
                .contains(BookingRefusal.HOURS_OUT_OF_BOUNDS);
    }

    @Test
    @Tag("OFR-5")
    @DisplayName("With 3 active helpers, customers can book 1 to 3 helpers; a lower maximum or a higher minimum narrows that.")
    void helpersMustFallWithinTheBoundsAndTheActiveHelpers() {
        assertThat(kemasHome.admits(booking(saturday(10, 0), STANDARD_HOURS, 3), REQUESTED_AT)).isTrue();
        assertThat(kemasHome.refusalOf(booking(saturday(10, 0), STANDARD_HOURS, 4), REQUESTED_AT))
                .contains(BookingRefusal.HELPERS_OUT_OF_BOUNDS);

        BookableProvider atMostTwo = kemasHomeProvider()
                .bounds(new BookingBounds(Duration.ofHours(2), Duration.ofHours(6), BOOKING_STEP, 1, 2)).build();
        assertThat(atMostTwo.admits(booking(saturday(10, 0), STANDARD_HOURS, 2), REQUESTED_AT)).isTrue();
        assertThat(atMostTwo.refusalOf(booking(saturday(10, 0), STANDARD_HOURS, 3), REQUESTED_AT))
                .contains(BookingRefusal.HELPERS_OUT_OF_BOUNDS);

        BookableProvider atLeastTwo = kemasHomeProvider()
                .bounds(new BookingBounds(Duration.ofHours(2), Duration.ofHours(6), BOOKING_STEP, 2, 6)).build();
        assertThat(atLeastTwo.refusalOf(booking(saturday(10, 0), STANDARD_HOURS, 1), REQUESTED_AT))
                .contains(BookingRefusal.HELPERS_OUT_OF_BOUNDS);
    }

    @Test
    @Tag("PRV-6")
    @DisplayName("Siti, Nurul and Rina are all deactivated for a holiday week, so Kemas Home can't be booked.")
    void providerThatIsNotBookableAdmitsNoBooking() {
        BookableProvider onHoliday = kemasHomeProvider().notBookable().build();

        assertThat(onHoliday.refusalOf(standardBooking(), REQUESTED_AT)).contains(BookingRefusal.PROVIDER_NOT_BOOKABLE);
        assertRefused(() -> onHoliday.requireAdmits(standardBooking(), REQUESTED_AT),
                BookingRefusal.PROVIDER_NOT_BOOKABLE);
        assertThatCode(() -> kemasHome.requireAdmits(standardBooking(), REQUESTED_AT)).doesNotThrowAnyException();
    }

    @Test
    @Tag("ACC-3")
    @DisplayName("A Request must be made at least 1 hour before the Check-in Period starts: 8:45 for 9:45 is just in time, 8:46 is too late.")
    void requestMustBeMadeAtLeastOneHourBeforeCheckIn() {
        BookableProvider shortNotice = kemasHomeProvider().minimumNotice(Duration.ofMinutes(30)).build();

        assertThat(shortNotice.admits(standardBooking(), saturday(8, 45))).isTrue();
        assertThat(shortNotice.refusalOf(standardBooking(), saturday(8, 46)))
                .contains(BookingRefusal.TOO_LATE_FOR_ACCEPTANCE_POLICY);
    }

    @Test
    @Tag("ACC-1")
    @DisplayName("An Instant reservation must be made before the Check-in Period starts: 9:44 is in time, 9:45 is too late.")
    void instantReservationMustBeMadeBeforeCheckIn() {
        BookableProvider instant = kemasHomeProvider().policy(AcceptancePolicy.INSTANT)
                .minimumNotice(Duration.ZERO).build();

        assertThat(instant.admits(standardBooking(), saturday(9, 44))).isTrue();
        assertThat(instant.refusalOf(standardBooking(), saturday(9, 45)))
                .contains(BookingRefusal.TOO_LATE_FOR_ACCEPTANCE_POLICY);
    }

    @Test
    @Tag("RES-9")
    @DisplayName("Kemas Home's 30-minute buffer makes the standard job block 9:45 to 14:30.")
    void calendarBlockUsesTheProvidersBuffer() {
        assertThat(kemasHome.calendarBlockFor(standardBooking().jobTime(), VALUES))
                .isEqualTo(new TimeInterval(saturday(9, 45), saturday(14, 30)));
        assertThat(kemasHomeProvider().buffer(Duration.ofHours(1)).build().calendarBlockFor(standardBooking().jobTime(), VALUES))
                .isEqualTo(new TimeInterval(saturday(9, 45), saturday(15, 0)));
    }

    @Test
    @Tag("RES-3")
    @DisplayName("The replica keeps what Listing and Provider said about Kemas Home.")
    void replicaKeepsTheProvidersSettings() {
        assertThat(kemasHome.providerId()).isEqualTo(KEMAS_HOME_PROVIDER_ID);
        assertThat(kemasHome.bookable()).isTrue();
        assertThat(kemasHome.cities()).containsExactlyInAnyOrderElementsOf(HomaFixtures.KEMAS_HOME_CITIES);
        assertThat(kemasHome.minimumNotice()).isEqualTo(HomaFixtures.MINIMUM_NOTICE);
        assertThat(kemasHome.buffer()).isEqualTo(HomaFixtures.JOB_BUFFER);
        assertThat(kemasHome.activeHelpers()).isEqualTo(3);
        assertThat(kemasHome.busyTimes()).isEmpty();
        assertThat(kemasHome.workingHours()).isEqualTo(ReservationFixtures.kemasWorkingHours());
    }

    private static Booking booking(Instant start, int hours) {
        return aisyahBooking(start, Duration.ofHours(hours), 2);
    }

    private static Booking booking(Instant start, Duration hours, int helpers) {
        return aisyahBooking(start, hours, helpers);
    }
}
