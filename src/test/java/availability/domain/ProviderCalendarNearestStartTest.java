package availability.domain;

import static availability.domain.AvailabilityBuilders.aCalendarAvailable;
import static availability.domain.AvailabilityBuilders.anOpenSaturday;
import static availability.domain.AvailabilityBuilders.busy;
import static availability.domain.AvailabilityBuilders.saturday;
import static availability.domain.AvailabilityBuilders.slot;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Finding the nearest free start, for close matches in search. */
class ProviderCalendarNearestStartTest {

    private static final Duration TWO_HOURS = Duration.ofHours(2);

    @Test
    void theWantedStartIsNearestWhenItIsFree() {
        assertThat(anOpenSaturday().nearestAvailableStart(slot("10:00", "13:00"), TWO_HOURS)).contains(saturday("10:00"));
    }

    @Test
    void aLaterStartIsFoundWhenTheWantedOneIsTaken() {
        ProviderCalendar calendar = aCalendarAvailable("11:00", "14:00");

        assertThat(calendar.nearestAvailableStart(slot("10:00", "13:00"), TWO_HOURS)).contains(saturday("11:00"));
    }

    @Test
    void anEarlierStartIsFoundWhenTheWantedOneIsTaken() {
        ProviderCalendar calendar = aCalendarAvailable("08:00", "12:15");

        assertThat(calendar.nearestAvailableStart(slot("10:00", "13:00"), TWO_HOURS)).contains(saturday("09:15"));
    }

    @Test
    void theStartIsSearchedInQuarterHourSteps() {
        ProviderCalendar calendar = aCalendarAvailable("10:15", "18:00");

        assertThat(calendar.nearestAvailableStart(slot("10:00", "13:00"), TWO_HOURS)).contains(saturday("10:15"));
    }

    @Test
    void theLaterStartWinsWhenTwoAreEquallyNear() {
        ProviderCalendar calendar = anOpenSaturday();
        calendar.replaceBusyBlocks(List.of(busy("10:00", "10:30")));

        assertThat(calendar.nearestAvailableStart(slot("10:00", "11:00"), TWO_HOURS)).contains(saturday("10:30"));

        calendar.replaceBusyBlocks(List.of(busy("10:15", "10:30")));
        assertThat(calendar.nearestAvailableStart(slot("10:00", "11:00"), TWO_HOURS)).contains(saturday("10:30"));
    }

    @Test
    void aStartExactlyAtTheEdgeOfTheSearchIsFound() {
        assertThat(aCalendarAvailable("12:00", "15:00").nearestAvailableStart(slot("10:00", "13:00"), TWO_HOURS))
                .contains(saturday("12:00"));
        assertThat(aCalendarAvailable("08:00", "11:00").nearestAvailableStart(slot("10:00", "13:00"), TWO_HOURS))
                .contains(saturday("08:00"));
    }

    @Test
    void nothingIsFoundBeyondTheSearch() {
        ProviderCalendar calendar = aCalendarAvailable("12:15", "18:00");

        assertThat(calendar.nearestAvailableStart(slot("10:00", "13:00"), TWO_HOURS)).isEmpty();
    }
}
