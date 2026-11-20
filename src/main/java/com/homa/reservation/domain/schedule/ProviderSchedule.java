package com.homa.reservation.domain.schedule;

import com.homa.reservation.domain.BookingRefusal;
import java.util.Objects;

/**
 * RES-9: a provider can have only one reservation at a time. Each Requested or Reserved reservation blocks the
 * provider from the start of its Check-in Period until the job end plus the provider's buffer. Aggregate root.
 */
public final class ProviderSchedule extends Schedule {

    private final String providerId;

    public ProviderSchedule(String providerId) {
        super(BookingRefusal.PROVIDER_BUSY);
        this.providerId = Objects.requireNonNull(providerId, "providerId");
    }

    public String providerId() {
        return providerId;
    }
}
