package availability.usecase;

import availability.domain.ProviderCalendar;
import availability.usecase.dto.ViewCalendarRequest;
import availability.usecase.dto.ViewCalendarResponse;
import availability.usecase.port.in.ProviderCalendarViewer;
import availability.usecase.port.out.ProviderCalendarRepository;

public final class ProviderViewCalendar implements ProviderCalendarViewer {

    private final ProviderCalendarRepository calendars;

    public ProviderViewCalendar(ProviderCalendarRepository calendars) {
        this.calendars = calendars;
    }

    @Override
    public ViewCalendarResponse view(ViewCalendarRequest request) {
        ProviderCalendar calendar = Calendars.calendar(calendars, request.providerId());
        return new ViewCalendarResponse(calendar.slots(), calendar.events(), calendar.buffer(), calendar.openTime());
    }
}
