package com.homa.rating.usecase;

import com.homa.rating.domain.Party;
import com.homa.rating.domain.Rating;
import com.homa.rating.domain.RatingSession;
import com.homa.rating.domain.RatingSessionStatus;
import com.homa.rating.domain.ReservationId;
import com.homa.rating.usecase.exception.RatingSessionChangedException;
import com.homa.rating.usecase.port.out.RatingSessionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Test only: sessions by reservation, and every save in order. The queries mirror what infra's SQL will return; the
 * rules they apply are the domain's. A test can make the next save of a session lose to someone else's.
 */
public class InMemoryRatingSessionRepository implements RatingSessionRepository {

    private final Map<ReservationId, RatingSession> sessions = new LinkedHashMap<>();
    private final List<RatingSession> saves = new ArrayList<>();
    private final Set<ReservationId> conflicting = new HashSet<>();

    /** Puts a session in place without counting it as a save. */
    public void given(RatingSession session) {
        sessions.put(session.reservationId(), session);
    }

    /** The next save of this reservation's session finds that someone else saved it first. */
    public void conflictOnNextSave(ReservationId reservationId) {
        conflicting.add(reservationId);
    }

    @Override
    public Optional<RatingSession> findByReservationId(ReservationId reservationId) {
        return Optional.ofNullable(sessions.get(reservationId));
    }

    @Override
    public List<ReservationId> findIdsOfOpenWithWindowClosedBy(Instant now) {
        return sessions.values().stream()
                .filter(session -> session.status() == RatingSessionStatus.OPEN)
                .filter(session -> session.window().hasClosedBy(now))
                .sorted(Comparator.comparing(session -> session.window().closesAt()))
                .map(RatingSession::reservationId)
                .toList();
    }

    @Override
    public List<Rating> findRevealedRatingsAbout(Party subject) {
        return sessions.values().stream()
                .map(session -> session.revealedRatingAbout(subject))
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public void save(RatingSession session) {
        if (conflicting.remove(session.reservationId())) {
            throw new RatingSessionChangedException(session.reservationId().value());
        }
        sessions.put(session.reservationId(), session);
        saves.add(session);
    }

    public List<RatingSession> saves() {
        return List.copyOf(saves);
    }

    public List<RatingSession> all() {
        return List.copyOf(sessions.values());
    }
}
