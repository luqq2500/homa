package availability.usecase.port.in;

import availability.usecase.dto.ViewCalendarRequest;
import availability.usecase.dto.ViewCalendarResponse;

/** A provider sees their availability, synced events, buffer and the time left open. */
public interface ProviderCalendarViewer {

    ViewCalendarResponse view(ViewCalendarRequest request);
}
