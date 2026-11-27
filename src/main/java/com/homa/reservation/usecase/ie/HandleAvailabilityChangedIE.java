package com.homa.reservation.usecase.ie;

import com.homa.ie.events.AvailabilityChangedIE;
import com.homa.reservation.domain.replica.DailyHours;
import com.homa.reservation.domain.replica.ProviderReplica;
import com.homa.reservation.domain.replica.WorkingHours;
import com.homa.reservation.usecase.port.in.AvailabilityChangedIEHandler;
import com.homa.reservation.usecase.port.out.BookableProviderRepository;
import com.homa.reservation.usecase.port.out.ProcessedMessageStore;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OFR-8, RES-3: Listing published the provider's weekly working hours anew, one "DAY HH:MM-HH:MM" entry per day in
 * Kuala Lumpur time. Only the provider replica changes; booked reservations stay as they are (RES-8).
 */
public final class HandleAvailabilityChangedIE implements AvailabilityChangedIEHandler {

    static final String HANDLER = "reservation.AvailabilityChanged";

    private final ProcessedMessageStore processed;
    private final BookableProviderRepository providers;

    public HandleAvailabilityChangedIE(ProcessedMessageStore processed, BookableProviderRepository providers) {
        this.processed = processed;
        this.providers = providers;
    }

    @Override
    public void handle(AvailabilityChangedIE event) {
        if (!processed.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        Map<DayOfWeek, DailyHours> days = event.weeklyHours().stream()
                .map(entry -> entry.split("[ -]"))
                .collect(Collectors.toMap(parts -> DayOfWeek.valueOf(parts[0]),
                        parts -> new DailyHours(LocalTime.parse(parts[1]), LocalTime.parse(parts[2]))));
        providers.save(providers.findById(event.providerId())
                .orElseGet(() -> ProviderReplica.unheardOf(event.providerId()))
                .working(new WorkingHours(days)));
    }
}
