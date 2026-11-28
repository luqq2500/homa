package com.homa.provider.usecase;

import com.homa.kernel.TimeInterval;
import com.homa.provider.domain.ReservationId;
import com.homa.provider.domain.RosteredReservation;
import com.homa.provider.domain.RosteredWindows;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.usecase.port.out.RosteredWindowsRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The rostered-reservation replica in a map. Holds no rule. */
public final class InMemoryRosteredWindowsRepository implements RosteredWindowsRepository {

    private final Map<ReservationId, RosteredReservation> stored = new LinkedHashMap<>();

    @Override
    public Optional<RosteredReservation> findByReservationId(ReservationId reservationId) {
        return Optional.ofNullable(stored.get(reservationId));
    }

    @Override
    public void save(RosteredReservation reservation) {
        stored.put(reservation.reservationId(), reservation);
    }

    @Override
    public void delete(ReservationId reservationId) {
        stored.remove(reservationId);
    }

    @Override
    public RosteredWindows windowsOf(TeamMemberId member) {
        List<TimeInterval> windows = stored.values().stream()
                .flatMap(reservation -> reservation.windowOf(member).stream())
                .toList();
        return RosteredWindows.of(windows);
    }

    /** How many reservations are kept, for assertions. */
    public int count() {
        return stored.size();
    }
}
