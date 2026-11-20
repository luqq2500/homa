package com.homa.ie.events;

import com.homa.ie.port.ListingIE;
import java.time.Instant;
import java.util.List;

/**
 * A provider's weekly working hours changed (OFR-8). Reservation keeps them in BookableProvider for RES-3.
 *
 * @param weeklyHours one entry per working period, each "DAY HH:MM-HH:MM", e.g. "SATURDAY 08:00-18:00",
 *                    in Asia/Kuala_Lumpur local time
 */
public record AvailabilityChangedIE(
        String eventId,
        String providerId, List<String> weeklyHours, Instant occurredAt)
        implements ListingIE {

    public AvailabilityChangedIE {
        weeklyHours = List.copyOf(weeklyHours);
    }
}
