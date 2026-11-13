package availability.usecase;

import availability.domain.ProviderCalendar;
import availability.usecase.port.out.AvailabilityIEPublisher;
import availability.usecase.port.out.ProviderCalendarRepository;
import ie.events.ProviderCalendarChangedIE;
import java.time.Instant;

/** What the calendar usecases share. */
final class Calendars {

    private Calendars() {
    }

    /** The provider's calendar; a provider who has set nothing yet has an empty one. */
    static ProviderCalendar calendar(ProviderCalendarRepository calendars, String providerId) {
        return calendars.find(providerId).orElseGet(() -> ProviderCalendar.startFor(providerId));
    }

    static void saveAndAnnounce(ProviderCalendar calendar, ProviderCalendarRepository calendars,
                                AvailabilityIEPublisher publisher, Instant now) {
        calendars.save(calendar);
        publisher.publish(
                new ProviderCalendarChangedIE(calendar.providerId(), calendar.openTime(), calendar.buffer(), now));
    }
}
