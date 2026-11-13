package availability.usecase.port.in;

import availability.usecase.dto.SyncCalendarEventsRequest;
import availability.usecase.dto.SyncCalendarEventsResponse;

/** The provider's own synced calendar reports the events that block their time. */
public interface CalendarEventsSyncer {

    SyncCalendarEventsResponse sync(SyncCalendarEventsRequest request);
}
