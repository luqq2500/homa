package com.homa.listing.usecase.port.out;

import com.homa.listing.domain.AvailabilitySchedule;
import com.homa.listing.domain.ProviderId;
import java.util.Optional;

/** Providers' availability schedules, one per provider (OFR-8). */
public interface AvailabilityRepository {

    Optional<AvailabilitySchedule> findById(ProviderId providerId);

    void save(AvailabilitySchedule schedule);
}
