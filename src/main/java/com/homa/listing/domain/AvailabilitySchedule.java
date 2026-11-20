package com.homa.listing.domain;

import com.homa.kernel.TimeInterval;
import java.time.ZoneId;
import java.util.Objects;

/**
 * When a provider can be booked (OFR-8). Slice 1 holds the weekly working hours that Reservation needs for RES-3.
 * Busy time synced from external calendars (the rest of OFR-8) joins in slice 8 as a second part of this aggregate
 * (a set of busy {@link TimeInterval}s replaced on each sync), and {@link #isAvailableFor} will then also refuse a
 * job time that overlaps busy time.
 */
public final class AvailabilitySchedule {

    private final ProviderId providerId;
    private WeeklyHours weeklyHours;

    private AvailabilitySchedule(ProviderId providerId, WeeklyHours weeklyHours) {
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.weeklyHours = Objects.requireNonNull(weeklyHours, "weeklyHours");
    }

    public static AvailabilitySchedule create(ProviderId providerId, WeeklyHours weeklyHours) {
        return new AvailabilitySchedule(providerId, weeklyHours);
    }

    public ProviderId providerId() {
        return providerId;
    }

    public WeeklyHours weeklyHours() {
        return weeklyHours;
    }

    /** The provider sets its working days and hours, replacing the previous ones. */
    public void setWeeklyHours(WeeklyHours changed) {
        weeklyHours = Objects.requireNonNull(changed, "weeklyHours");
    }

    /** True when the job time falls within the provider's working hours, read in the given zone. */
    public boolean isAvailableFor(TimeInterval jobTime, ZoneId zone) {
        return weeklyHours.covers(jobTime, zone);
    }
}
