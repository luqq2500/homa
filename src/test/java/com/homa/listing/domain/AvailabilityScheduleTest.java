package com.homa.listing.domain;

import static com.homa.fixtures.HomaFixtures.JOB_TIME;
import static com.homa.fixtures.HomaFixtures.KUALA_LUMPUR;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.listing.domain.ListingFixtures.EIGHT_TO_SIX;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME;
import static com.homa.listing.domain.ListingFixtures.kemasHomeAvailability;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.TimeInterval;
import com.homa.listing.domain.exception.InvalidWorkingHoursException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("OFR-8")
class AvailabilityScheduleTest {

    private static final LocalTime EIGHT = LocalTime.of(8, 0);

    @Test
    @DisplayName("Kemas Home works Monday–Saturday, 8:00–18:00, so a job can start at 8:00 but can't run 17:00–19:00")
    void aJobMustFallWithinTheProvidersWorkingHours() {
        AvailabilitySchedule kemasHome = kemasHomeAvailability();

        assertThat(kemasHome.isAvailableFor(JOB_TIME, KUALA_LUMPUR)).isTrue();
        assertThat(kemasHome.isAvailableFor(saturday(8, 0, 10, 0), KUALA_LUMPUR)).isTrue();
        assertThat(kemasHome.isAvailableFor(saturday(16, 0, 18, 0), KUALA_LUMPUR)).isTrue();
        assertThat(kemasHome.isAvailableFor(saturday(17, 0, 19, 0), KUALA_LUMPUR)).isFalse();
        assertThat(kemasHome.isAvailableFor(saturday(7, 45, 10, 0), KUALA_LUMPUR)).isFalse();
    }

    @Test
    @DisplayName("Kemas Home doesn't work on Sunday")
    void aDayWithoutHoursIsNotAWorkingDay() {
        TimeInterval sunday = new TimeInterval(at(2026, 12, 6, 10, 0), at(2026, 12, 6, 14, 0));

        assertThat(kemasHomeAvailability().isAvailableFor(sunday, KUALA_LUMPUR)).isFalse();
        assertThat(kemasHomeAvailability().weeklyHours().hoursOn(DayOfWeek.SUNDAY)).isEmpty();
        assertThat(kemasHomeAvailability().weeklyHours().hoursOn(DayOfWeek.SATURDAY)).contains(EIGHT_TO_SIX);
    }

    @Test
    @DisplayName("A job running past midnight doesn't fit in one working day, even with round-the-clock hours")
    void aJobMustFitInOneWorkingDay() {
        WeeklyHours allDay = WeeklyHours.on(Set.of(DayOfWeek.values()), new WorkingHours(LocalTime.MIN, LocalTime.MAX));
        TimeInterval overnight = new TimeInterval(at(2026, 12, 5, 22, 0), at(2026, 12, 6, 1, 0));

        assertThat(allDay.covers(overnight, KUALA_LUMPUR)).isFalse();
        assertThat(allDay.covers(saturday(22, 0, 23, 0), KUALA_LUMPUR)).isTrue();
    }

    @Test
    @DisplayName("Kemas Home moves Saturday to 9:00–13:00; a 10:00–14:00 job no longer fits")
    void aProviderChangesItsWeeklyHours() {
        AvailabilitySchedule kemasHome = kemasHomeAvailability();

        kemasHome.setWeeklyHours(WeeklyHours.on(Set.of(DayOfWeek.SATURDAY), new WorkingHours(LocalTime.of(9, 0),
                LocalTime.of(13, 0))));

        assertThat(kemasHome.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(kemasHome.isAvailableFor(JOB_TIME, KUALA_LUMPUR)).isFalse();
        assertThat(kemasHome.isAvailableFor(saturday(9, 0, 13, 0), KUALA_LUMPUR)).isTrue();
        assertThatThrownBy(() -> kemasHome.setWeeklyHours(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Working hours must end after they start")
    void workingHoursMustEndAfterTheyStart() {
        assertThat(new WorkingHours(EIGHT, EIGHT.plusMinutes(1)).end()).isEqualTo(LocalTime.of(8, 1));
        assertThatThrownBy(() -> new WorkingHours(EIGHT, EIGHT)).isInstanceOf(InvalidWorkingHoursException.class);
        assertThatThrownBy(() -> new WorkingHours(EIGHT, LocalTime.of(7, 0))).isInstanceOf(InvalidWorkingHoursException.class);
        assertThatThrownBy(() -> AvailabilitySchedule.create(null, ListingFixtures.kemasHomeWeeklyHours()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Persistence: Kemas Home's stored schedule comes back with its working hours, and they can still be changed")
    void aStoredScheduleIsRebuiltWithItsWorkingHours() {
        AvailabilitySchedule kemasHome = kemasHomeAvailability();

        AvailabilitySchedule stored = AvailabilitySchedule.rebuild(kemasHome.providerId(), kemasHome.weeklyHours());

        assertThat(stored).usingRecursiveComparison().isEqualTo(kemasHome);
        assertThat(stored.isAvailableFor(JOB_TIME, KUALA_LUMPUR)).isTrue();
        stored.setWeeklyHours(WeeklyHours.on(Set.of(DayOfWeek.MONDAY), EIGHT_TO_SIX));
        assertThat(stored.isAvailableFor(JOB_TIME, KUALA_LUMPUR)).isFalse();
        assertThatThrownBy(() -> AvailabilitySchedule.rebuild(KEMAS_HOME, null))
                .isInstanceOf(NullPointerException.class);
    }

    private static TimeInterval saturday(int startHour, int startMinute, int endHour, int endMinute) {
        return new TimeInterval(at(2026, 12, 5, startHour, startMinute), at(2026, 12, 5, endHour, endMinute));
    }
}
