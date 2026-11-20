package com.homa.reservation.domain;

import com.homa.reservation.domain.replica.BookableProvider;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * The job start times a customer can choose on a date: in 15-minute steps within the provider's working hours,
 * admitted by the provider's booking rules (RES-3 to RES-5) and free in its schedule (RES-9, RES-11).
 */
public final class AvailableStartTimes {

    private AvailableStartTimes() {
    }

    public static List<Instant> on(
            LocalDate date, Booking booking, BookableProvider provider, ProviderSchedule schedule, Instant now) {
        return provider.workingHours().startsOn(date, booking.jobTime().duration()).stream()
                .map(booking::startingAt)
                .filter(candidate -> provider.admits(candidate, now))
                .filter(candidate -> schedule.isFree(provider.calendarBlockFor(candidate.jobTime()), now))
                .map(candidate -> candidate.jobTime().start())
                .toList();
    }
}
