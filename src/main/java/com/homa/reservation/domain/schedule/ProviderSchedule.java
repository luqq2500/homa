package com.homa.reservation.domain.schedule;

import com.homa.reservation.domain.BookingRefusal;
import java.util.List;
import java.util.Objects;

/**
 * RES-9: a provider can have only one reservation at a time. Each Requested or Reserved reservation blocks the
 * provider from the start of its Check-in Period until the job end plus the provider's buffer. A payment confirmed
 * for a taken time is refused with {@link BookingRefusal#TIME_TAKEN_BY_ANOTHER_RESERVATION} (RES-11). Aggregate
 * root.
 */
public final class ProviderSchedule extends Schedule {

    private final String providerId;

    /** A provider's schedule with nothing booked yet. */
    public ProviderSchedule(String providerId) {
        this(providerId, List.of(), 0);
    }

    /** A provider's schedule as it was saved, with its blocks and the version it was saved at. */
    public ProviderSchedule(String providerId, List<ScheduleBlock> blocks, long version) {
        super(blocks, version, BookingRefusal.TIME_TAKEN_BY_ANOTHER_RESERVATION);
        this.providerId = Objects.requireNonNull(providerId, "providerId");
    }

    public String providerId() {
        return providerId;
    }
}
