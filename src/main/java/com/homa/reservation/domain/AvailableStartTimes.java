package com.homa.reservation.domain;

import com.homa.reservation.domain.replica.BookableProvider;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * The job start times a customer can choose on a date: in the provider's booking steps (15 minutes, from Listing)
 * within its working hours, admitted by its booking rules (RES-3 to RES-5) and free in its schedule now (RES-9). A
 * time shown free is not held: the first confirmed payment for it wins (RES-11).
 */
public final class AvailableStartTimes {

    private AvailableStartTimes() {
    }

    public static List<Instant> on(
            LocalDate date, Booking booking, BookableProvider provider, ProviderSchedule schedule, Instant now) {
        ReservationValues.Version values = ReservationValues.asOf(now);
        return provider.workingHours().startsOn(date, booking.jobTime().duration(), provider.bounds().step()).stream()
                .map(booking::startingAt)
                .filter(candidate -> provider.admits(candidate, now))
                .filter(candidate -> schedule.isFree(provider.calendarBlockFor(candidate.jobTime(), values)))
                .map(candidate -> candidate.jobTime().start())
                .toList();
    }
}
