package com.homa.listing.usecase;

import com.homa.listing.domain.AvailabilitySchedule;
import com.homa.listing.domain.ProviderId;
import com.homa.listing.domain.WeeklyHours;
import com.homa.listing.usecase.port.out.AvailabilityRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class InMemoryAvailabilityRepository implements AvailabilityRepository {

    private final Map<ProviderId, WeeklyHours> stored = new HashMap<>();

    @Override
    public Optional<AvailabilitySchedule> findById(ProviderId providerId) {
        return Optional.ofNullable(stored.get(providerId)).map(hours -> AvailabilitySchedule.rebuild(providerId, hours));
    }

    @Override
    public void save(AvailabilitySchedule schedule) {
        stored.put(schedule.providerId(), schedule.weeklyHours());
    }

    Optional<WeeklyHours> weeklyHoursOf(ProviderId providerId) {
        return Optional.ofNullable(stored.get(providerId));
    }
}
