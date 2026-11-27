package com.homa.listing.usecase;

import com.homa.listing.domain.AvailabilitySchedule;
import com.homa.listing.domain.ProviderId;
import com.homa.listing.domain.WeeklyHours;
import com.homa.listing.usecase.dto.ProviderSetAvailabilityRequest;
import com.homa.listing.usecase.dto.ProviderSetAvailabilityResponse;
import com.homa.listing.usecase.port.in.AvailabilitySetter;
import com.homa.listing.usecase.port.out.AvailabilityRepository;
import com.homa.listing.usecase.port.out.ListingIEPublisher;
import com.homa.listing.usecase.port.out.ListingOwners;

/**
 * OFR-8, weekly hours only: the provider's working days and hours replace the previous ones, and are published as
 * {@code AvailabilityChangedIE}. Busy time synced from external calendars comes in slice 8.
 */
public final class ProviderSetAvailability implements AvailabilitySetter {

    private final AvailabilityRepository availability;
    private final ListingOwners owners;
    private final ListingIEPublisher publisher;
    private final ListingEvents events;

    public ProviderSetAvailability(AvailabilityRepository availability, ListingOwners owners,
                                   ListingIEPublisher publisher, ListingEvents events) {
        this.availability = availability;
        this.owners = owners;
        this.publisher = publisher;
        this.events = events;
    }

    @Override
    public ProviderSetAvailabilityResponse set(ProviderSetAvailabilityRequest request) {
        ProviderId providerId = new ProviderId(request.providerId());
        Ownership.requireOwner(owners, providerId, request.actingPersonId());
        WeeklyHours weeklyHours = WeeklyHoursFormat.parse(request.weeklyHours());
        AvailabilitySchedule schedule = availability.findById(providerId)
                .orElseGet(() -> AvailabilitySchedule.create(providerId, weeklyHours));
        schedule.setWeeklyHours(weeklyHours);
        availability.save(schedule);
        publisher.publish(events.availabilityChanged(schedule));
        return new ProviderSetAvailabilityResponse(request.providerId(), WeeklyHoursFormat.format(schedule.weeklyHours()));
    }
}
