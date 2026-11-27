package com.homa.reservation.usecase;

import com.homa.reservation.domain.AvailableStartTimes;
import com.homa.reservation.domain.Booking;
import com.homa.reservation.domain.JobSelection;
import com.homa.reservation.domain.JobTime;
import com.homa.reservation.domain.replica.BookableProvider;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import com.homa.reservation.usecase.dto.FindAvailableStartTimesRequest;
import com.homa.reservation.usecase.dto.FindAvailableStartTimesResponse;
import com.homa.reservation.usecase.exception.CustomerNotFoundException;
import com.homa.reservation.usecase.port.in.AvailableStartFinder;
import com.homa.reservation.usecase.port.out.CustomerStandingRepository;
import com.homa.reservation.usecase.port.out.ProviderScheduleRepository;
import com.homa.reservation.usecase.port.out.TimeSource;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * RES-3, RES-4, RES-5, RES-9: the job start times the customer can choose on a date, for a booking at their home
 * address. A time shown free is not held (RES-11).
 */
public final class FindAvailableStartTimes implements AvailableStartFinder {

    private final CustomerStandingRepository customers;
    private final BookableProviders providers;
    private final ProviderScheduleRepository providerSchedules;
    private final TimeSource time;

    public FindAvailableStartTimes(CustomerStandingRepository customers, BookableProviders providers,
            ProviderScheduleRepository providerSchedules, TimeSource time) {
        this.customers = customers;
        this.providers = providers;
        this.providerSchedules = providerSchedules;
        this.time = time;
    }

    @Override
    public FindAvailableStartTimesResponse find(FindAvailableStartTimesRequest request) {
        Instant now = time.now();
        CustomerStanding customer = customers.findById(request.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(request.customerId()));
        BookableProvider provider = providers.find(request.providerId());
        ProviderSchedule schedule = providerSchedules.findByProviderId(request.providerId())
                .orElseGet(() -> new ProviderSchedule(request.providerId()));
        Booking booking = new Booking(customer.customerId(), provider.providerId(),
                new JobSelection(Set.copyOf(request.jobCodes())),
                JobTime.of(now, Duration.ofMinutes(request.minutes())), request.helpers(), customer.homeAddress());
        return new FindAvailableStartTimesResponse(
                AvailableStartTimes.on(request.date(), booking, provider, schedule, now));
    }
}
