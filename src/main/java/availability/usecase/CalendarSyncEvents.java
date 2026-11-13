package availability.usecase;

import availability.domain.ProviderCalendar;
import availability.usecase.dto.SyncCalendarEventsRequest;
import availability.usecase.dto.SyncCalendarEventsResponse;
import availability.usecase.port.in.CalendarEventsSyncer;
import availability.usecase.port.out.AvailabilityIEPublisher;
import availability.usecase.port.out.ProviderCalendarRepository;
import java.time.Clock;

public final class CalendarSyncEvents implements CalendarEventsSyncer {

    private final ProviderCalendarRepository calendars;
    private final AvailabilityIEPublisher publisher;
    private final Clock clock;

    public CalendarSyncEvents(ProviderCalendarRepository calendars, AvailabilityIEPublisher publisher, Clock clock) {
        this.calendars = calendars;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public SyncCalendarEventsResponse sync(SyncCalendarEventsRequest request) {
        ProviderCalendar calendar = Calendars.calendar(calendars, request.providerId());
        calendar.syncEvents(request.events());
        Calendars.saveAndAnnounce(calendar, calendars, publisher, clock.instant());
        return new SyncCalendarEventsResponse(calendar.openTime());
    }
}
