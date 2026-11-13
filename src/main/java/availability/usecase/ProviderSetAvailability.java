package availability.usecase;

import availability.domain.ProviderCalendar;
import availability.usecase.dto.SetAvailabilityRequest;
import availability.usecase.dto.SetAvailabilityResponse;
import availability.usecase.port.in.ProviderAvailabilitySetter;
import availability.usecase.port.out.AvailabilityIEPublisher;
import availability.usecase.port.out.ProviderCalendarRepository;
import java.time.Clock;

public final class ProviderSetAvailability implements ProviderAvailabilitySetter {

    private final ProviderCalendarRepository calendars;
    private final AvailabilityIEPublisher publisher;
    private final Clock clock;

    public ProviderSetAvailability(ProviderCalendarRepository calendars, AvailabilityIEPublisher publisher, Clock clock) {
        this.calendars = calendars;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public SetAvailabilityResponse set(SetAvailabilityRequest request) {
        ProviderCalendar calendar = Calendars.calendar(calendars, request.providerId());
        calendar.setAvailability(request.slots());
        Calendars.saveAndAnnounce(calendar, calendars, publisher, clock.instant());
        return new SetAvailabilityResponse(calendar.openTime());
    }
}
