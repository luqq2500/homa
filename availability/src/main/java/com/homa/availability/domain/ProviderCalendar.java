package com.homa.availability.domain;

import com.homa.availability.domain.exception.AvailabilityDetailRequiredException;
import com.homa.availability.domain.exception.NoHoldForReservationException;
import com.homa.availability.domain.exception.ReservationAlreadyHeldException;
import com.homa.availability.domain.exception.SlotNotAvailableException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * When a provider can work. A slot is available when it sits inside the provider's availability windows
 * and touches no synced busy block and no slot held by an accepted reservation.
 */
public final class ProviderCalendar {

    private final String id;
    private final String providerId;
    private List<AvailabilityWindow> windows = List.of();
    private List<BusyBlock> busyBlocks = List.of();
    private final Map<String, ReservationHold> holds = new LinkedHashMap<>();

    private ProviderCalendar(String id, String providerId) {
        this.id = id;
        this.providerId = providerId;
    }

    public static ProviderCalendar of(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            throw new AvailabilityDetailRequiredException("A provider id");
        }
        return new ProviderCalendar(new ProviderCalendarId().generate(), providerId);
    }

    /** Replaces the provider's availability windows. Windows that touch or overlap count as one stretch. */
    public void setWindows(List<AvailabilityWindow> newWindows) {
        windows = newWindows.stream().sorted(Comparator.comparing(AvailabilityWindow::start)).toList();
    }

    /** Replaces the busy blocks with the latest calendar sync. Each sync replaces the last. */
    public void replaceBusyBlocks(List<BusyBlock> blocks) {
        busyBlocks = List.copyOf(blocks);
    }

    /** Holds {@code slot} for an accepted reservation. */
    public void hold(String reservationId, TimeSlot slot) {
        if (holds.containsKey(reservationId)) {
            throw new ReservationAlreadyHeldException(reservationId);
        }
        if (!isAvailable(slot)) {
            throw new SlotNotAvailableException(slot);
        }
        holds.put(reservationId, new ReservationHold(reservationId, slot));
    }

    /** Frees the slot a cancelled reservation held. */
    public void release(String reservationId) {
        if (holds.remove(reservationId) == null) {
            throw new NoHoldForReservationException(reservationId);
        }
    }

    public boolean isAvailable(TimeSlot slot) {
        return insideWindows(slot) && blockers().noneMatch(slot::overlaps);
    }

    /**
     * The available start nearest to the wanted one, for a slot of the same length, looking in 15-minute
     * steps up to {@code within} before or after. When two starts are equally near, the later one is
     * chosen.
     */
    public Optional<Instant> nearestAvailableStart(TimeSlot wanted, Duration within) {
        long steps = within.dividedBy(TimeSlot.STEP);
        for (long step = 0; step <= steps; step++) {
            Duration offset = TimeSlot.STEP.multipliedBy(step);
            Optional<Instant> found = firstAvailable(wanted, wanted.start().plus(offset), wanted.start().minus(offset));
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    public List<AvailabilityWindow> windows() {
        return windows;
    }

    public List<BusyBlock> busyBlocks() {
        return busyBlocks;
    }

    public List<ReservationHold> holds() {
        return List.copyOf(holds.values());
    }

    public String id() {
        return id;
    }

    public String providerId() {
        return providerId;
    }

    private Optional<Instant> firstAvailable(TimeSlot wanted, Instant... starts) {
        return Stream.of(starts).filter(start -> isAvailable(wanted.startingAt(start))).findFirst();
    }

    private Stream<TimeRange> blockers() {
        return Stream.concat(busyBlocks.stream(), holds.values().stream().map(ReservationHold::slot));
    }

    /** Walks the sorted windows from the slot's start, until the slot's end is reached or a gap is found. */
    private boolean insideWindows(TimeSlot slot) {
        Instant reached = slot.start();
        for (AvailabilityWindow window : windows) {
            if (window.includes(reached)) {
                reached = window.end();
            }
        }
        return !reached.isBefore(slot.end());
    }
}
