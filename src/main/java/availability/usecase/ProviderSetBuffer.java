package availability.usecase;

import availability.domain.ProviderCalendar;
import availability.usecase.dto.SetBufferRequest;
import availability.usecase.dto.SetBufferResponse;
import availability.usecase.port.in.ProviderBufferSetter;
import availability.usecase.port.out.AvailabilityIEPublisher;
import availability.usecase.port.out.ProviderCalendarRepository;
import java.time.Clock;

public final class ProviderSetBuffer implements ProviderBufferSetter {

    private final ProviderCalendarRepository calendars;
    private final AvailabilityIEPublisher publisher;
    private final Clock clock;

    public ProviderSetBuffer(ProviderCalendarRepository calendars, AvailabilityIEPublisher publisher, Clock clock) {
        this.calendars = calendars;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public SetBufferResponse set(SetBufferRequest request) {
        ProviderCalendar calendar = Calendars.calendar(calendars, request.providerId());
        calendar.setBuffer(request.buffer());
        Calendars.saveAndAnnounce(calendar, calendars, publisher, clock.instant());
        return new SetBufferResponse(calendar.buffer());
    }
}
