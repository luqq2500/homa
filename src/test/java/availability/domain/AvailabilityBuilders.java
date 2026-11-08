package availability.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/** Test-data builders for calendars, read as domain sentences. */
final class AvailabilityBuilders {

    static final ZoneId MALAYSIA = ZoneId.of("Asia/Kuala_Lumpur");
    static final LocalDate SATURDAY = LocalDate.of(2026, 11, 14);

    private AvailabilityBuilders() {
    }

    /** A time on Saturday, written as "10:00". */
    static Instant saturday(String time) {
        return SATURDAY.atTime(LocalTime.parse(time)).atZone(MALAYSIA).toInstant();
    }

    static TimeSlot slot(String from, String to) {
        return new TimeSlot(saturday(from), saturday(to));
    }

    static AvailabilityWindow window(String from, String to) {
        return AvailabilityWindow.of(saturday(from), saturday(to));
    }

    static BusyBlock busy(String from, String to) {
        return BusyBlock.of(saturday(from), saturday(to));
    }

    static ProviderCalendar aCalendarAvailable(String from, String to) {
        ProviderCalendar calendar = ProviderCalendar.of("provider-1");
        calendar.setWindows(List.of(window(from, to)));
        return calendar;
    }

    /** Available all Saturday from 08:00 to 18:00. */
    static ProviderCalendar anOpenSaturday() {
        return aCalendarAvailable("08:00", "18:00");
    }
}
