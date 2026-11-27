package com.homa.reservation.usecase;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationId;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.schedule.CustomerSchedule;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import com.homa.reservation.usecase.exception.ScheduleChangedException;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reservations as a store keeps them. Every load and save is a copy, as a database round trip would be, so a use
 * case that changes a reservation and then fails to save leaves the stored one as it was. The schedules' optimistic
 * lock compares the version each schedule was loaded at with the stored one. A test can make the next saves
 * conflict, or let a competing save land just before the next one.
 */
public final class InMemoryReservationRepository implements ReservationRepository {

    private final Map<ReservationId, Reservation> stored = new LinkedHashMap<>();
    private final InMemorySchedules schedules;
    private int conflictsToRaise;
    private Runnable beforeNextSave;

    public InMemoryReservationRepository(InMemorySchedules schedules) {
        this.schedules = schedules;
    }

    /** The next {@code count} saves with schedules fail as if another save had moved a schedule on. */
    public void conflictOnNextSaves(int count) {
        this.conflictsToRaise = count;
    }

    /** Runs a competing step just before the next save with schedules, as another transaction committing first. */
    public void beforeNextSave(Runnable competing) {
        this.beforeNextSave = competing;
    }

    @Override
    public Optional<Reservation> findById(ReservationId id) {
        return Optional.ofNullable(stored.get(id)).map(InMemoryReservationRepository::copy);
    }

    @Override
    public List<Reservation> findByParty(String partyId) {
        return stored.values().stream()
                .filter(reservation -> reservation.booking().customerId().equals(partyId)
                        || reservation.booking().providerId().equals(partyId))
                .sorted(Comparator.comparing(reservation -> reservation.jobTime().start()))
                .map(InMemoryReservationRepository::copy)
                .toList();
    }

    @Override
    public List<Reservation> findReservedEndedBy(Instant now) {
        return stored.values().stream()
                .filter(reservation -> reservation.status() == ReservationStatus.RESERVED)
                .filter(reservation -> !reservation.jobTime().end().isAfter(now))
                .map(InMemoryReservationRepository::copy)
                .toList();
    }

    @Override
    public void save(Reservation reservation) {
        stored.put(reservation.id(), copy(reservation));
    }

    @Override
    public void saveWithSchedules(Reservation reservation, ProviderSchedule providerSchedule,
            CustomerSchedule customerSchedule) {
        Runnable competing = beforeNextSave;
        beforeNextSave = null;
        if (competing != null) {
            competing.run();
        }
        if (conflictsToRaise > 0) {
            conflictsToRaise--;
            throw new ScheduleChangedException(providerSchedule.providerId());
        }
        if (schedules.providerVersion(providerSchedule.providerId()) != providerSchedule.version()) {
            throw new ScheduleChangedException(providerSchedule.providerId());
        }
        if (schedules.customerVersion(customerSchedule.customerId()) != customerSchedule.version()) {
            throw new ScheduleChangedException(customerSchedule.customerId());
        }
        schedules.store(providerSchedule, customerSchedule);
        save(reservation);
    }

    public int count() {
        return stored.size();
    }

    /** The only stored reservation; fails unless there is exactly one. */
    public Reservation single() {
        if (stored.size() != 1) {
            throw new AssertionError("Expected one reservation, found " + stored.size());
        }
        return copy(stored.values().iterator().next());
    }

    /** A copy of the reservation with the same state, as a reload from the database gives. */
    static Reservation copy(Reservation original) {
        try {
            Constructor<?> constructor = Reservation.class.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            Object[] arguments = new Object[constructor.getParameterCount()];
            List<Field> fields = java.util.Arrays.stream(Reservation.class.getDeclaredFields())
                    .filter(field -> !Modifier.isStatic(field.getModifiers()))
                    .toList();
            for (int i = 0; i < arguments.length; i++) {
                fields.get(i).setAccessible(true);
                arguments[i] = fields.get(i).get(original);
            }
            Reservation copy = (Reservation) constructor.newInstance(arguments);
            for (Field field : fields) {
                field.setAccessible(true);
                if (!Modifier.isFinal(field.getModifiers())) {
                    field.set(copy, field.get(original));
                }
            }
            return copy;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot copy the reservation", e);
        }
    }
}
