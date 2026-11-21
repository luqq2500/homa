package com.homa.reservation.domain.replica;

import static com.homa.reservation.domain.BookingRefusal.CITY_NOT_COVERED;
import static com.homa.reservation.domain.BookingRefusal.HELPERS_OUT_OF_BOUNDS;
import static com.homa.reservation.domain.BookingRefusal.HOURS_OUT_OF_BOUNDS;
import static com.homa.reservation.domain.BookingRefusal.JOB_NOT_OFFERED;
import static com.homa.reservation.domain.BookingRefusal.NOTICE_TOO_SHORT;
import static com.homa.reservation.domain.BookingRefusal.OUTSIDE_WORKING_HOURS;
import static com.homa.reservation.domain.BookingRefusal.PROVIDER_BUSY;
import static com.homa.reservation.domain.BookingRefusal.PROVIDER_NOT_BOOKABLE;
import static com.homa.reservation.domain.BookingRefusal.TOO_LATE_FOR_ACCEPTANCE_POLICY;

import com.homa.kernel.TimeInterval;
import com.homa.reservation.domain.Booking;
import com.homa.reservation.domain.BookingRefusal;
import com.homa.reservation.domain.JobTime;
import com.homa.reservation.domain.ReservationValues;
import com.homa.reservation.domain.ReservationValues.Version;
import com.homa.reservation.domain.ServiceAddress;
import com.homa.reservation.domain.exception.BookingRefusedException;
import com.homa.reservation.domain.terms.ReservationTerms;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A provider as Reservation sees it: Listing's terms, settings and availability plus Provider's bookability and
 * active helper count, kept up to date from their events. It decides whether a booking passes the provider's
 * booking rules; the provider's own reservations are checked by the {@code ProviderSchedule}. A booking checked at a
 * moment is checked under the reservation values in force then, the ones it would be Requested with (P3).
 *
 * @param bookable    false while the provider can't be booked, for example with no active helpers (PRV-6)
 * @param busyTimes   private appointments synced from external calendars (OFR-8)
 */
public record BookableProvider(
        String providerId,
        boolean bookable,
        Set<String> cities,
        Set<String> jobs,
        OfferedTerms terms,
        BookingBounds bounds,
        int activeHelpers,
        Duration minimumNotice,
        Duration buffer,
        WorkingHours workingHours,
        List<TimeInterval> busyTimes) {

    public BookableProvider {
        Objects.requireNonNull(providerId, "providerId");
        cities = Set.copyOf(cities);
        jobs = Set.copyOf(jobs);
        Objects.requireNonNull(terms, "terms");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(minimumNotice, "minimumNotice");
        Objects.requireNonNull(buffer, "buffer");
        Objects.requireNonNull(workingHours, "workingHours");
        busyTimes = List.copyOf(busyTimes);
    }

    /** Whether the booking passes every booking rule the provider decides, at this moment. */
    public boolean admits(Booking booking, Instant now) {
        return refusalOf(booking, now).isEmpty();
    }

    /** The first booking rule the booking breaks, if any. */
    public Optional<BookingRefusal> refusalOf(Booking booking, Instant now) {
        return checks(now, ReservationValues.asOf(now)).stream()
                .filter(check -> !check.rule().test(booking))
                .map(BookingCheck::refusal)
                .findFirst();
    }

    public void requireAdmits(Booking booking, Instant now) {
        refusalOf(booking, now).ifPresent(refusal -> {
            throw new BookingRefusedException(refusal);
        });
    }

    /** RES-8: the terms a reservation at this address freezes when it is Requested. */
    public ReservationTerms termsFor(ServiceAddress address) {
        return terms.snapshotFor(address.city());
    }

    /** RES-9: the provider's time a job blocks, from the Check-in Period to the job end plus the buffer. */
    public TimeInterval calendarBlockFor(JobTime jobTime, Version values) {
        return jobTime.calendarBlock(buffer, values);
    }

    private List<BookingCheck> checks(Instant now, Version values) {
        return List.of(
                new BookingCheck(PROVIDER_NOT_BOOKABLE, booking -> bookable),
                new BookingCheck(CITY_NOT_COVERED, booking -> cities.contains(booking.address().city())),
                new BookingCheck(JOB_NOT_OFFERED, booking -> booking.jobs().isOfferedBy(jobs)),
                new BookingCheck(HOURS_OUT_OF_BOUNDS, booking -> booking.jobTime().duration().fits(bounds)),
                new BookingCheck(HELPERS_OUT_OF_BOUNDS, booking -> bounds.admitsHelpers(booking.helpers(), activeHelpers)),
                new BookingCheck(NOTICE_TOO_SHORT, booking -> hasEnoughNotice(booking.jobTime(), now, values)),
                new BookingCheck(TOO_LATE_FOR_ACCEPTANCE_POLICY, booking -> terms.acceptancePolicy()
                        .permitsNotice(booking.jobTime().noticeFrom(now, values), values)),
                new BookingCheck(OUTSIDE_WORKING_HOURS, booking -> workingHours.covers(booking.jobTime().interval())),
                new BookingCheck(PROVIDER_BUSY,
                        booking -> isFreeOfBusyTimes(calendarBlockFor(booking.jobTime(), values))));
    }

    /** RES-4: the Notice is at least the provider's minimum notice. */
    private boolean hasEnoughNotice(JobTime jobTime, Instant now, Version values) {
        return jobTime.noticeFrom(now, values).compareTo(minimumNotice) >= 0;
    }

    private boolean isFreeOfBusyTimes(TimeInterval block) {
        return busyTimes.stream().noneMatch(busy -> busy.overlaps(block));
    }

    private record BookingCheck(BookingRefusal refusal, Predicate<Booking> rule) {
    }
}
