package com.homa.reservation.usecase;

import com.homa.reservation.domain.schedule.CustomerSchedule;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import com.homa.reservation.domain.schedule.ScheduleBlock;
import com.homa.reservation.usecase.port.out.CustomerScheduleRepository;
import com.homa.reservation.usecase.port.out.ProviderScheduleRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Both parties' schedules as a store keeps them: each saved with its blocks and version, and every load is a fresh
 * copy at the stored version, as a database read would be.
 */
public final class InMemorySchedules implements ProviderScheduleRepository, CustomerScheduleRepository {

    private record Stored(List<ScheduleBlock> blocks, long version) {
    }

    private final Map<String, Stored> providers = new HashMap<>();
    private final Map<String, Stored> customers = new HashMap<>();

    @Override
    public Optional<ProviderSchedule> findByProviderId(String providerId) {
        return Optional.ofNullable(providers.get(providerId))
                .map(stored -> new ProviderSchedule(providerId, stored.blocks(), stored.version()));
    }

    @Override
    public Optional<CustomerSchedule> findByCustomerId(String customerId) {
        return Optional.ofNullable(customers.get(customerId))
                .map(stored -> new CustomerSchedule(customerId, stored.blocks(), stored.version()));
    }

    long providerVersion(String providerId) {
        return Optional.ofNullable(providers.get(providerId)).map(Stored::version).orElse(0L);
    }

    long customerVersion(String customerId) {
        return Optional.ofNullable(customers.get(customerId)).map(Stored::version).orElse(0L);
    }

    void store(ProviderSchedule schedule, CustomerSchedule customerSchedule) {
        providers.put(schedule.providerId(), new Stored(schedule.blocks(), schedule.version() + 1));
        customers.put(customerSchedule.customerId(),
                new Stored(customerSchedule.blocks(), customerSchedule.version() + 1));
    }

    /** The blocks of the provider's schedule as stored; empty when it has none. */
    public List<ScheduleBlock> providerBlocks(String providerId) {
        return findByProviderId(providerId).map(ProviderSchedule::blocks).orElse(List.of());
    }

    public List<ScheduleBlock> customerBlocks(String customerId) {
        return findByCustomerId(customerId).map(CustomerSchedule::blocks).orElse(List.of());
    }
}
