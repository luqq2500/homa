package com.homa.rating.usecase.port.out;

import com.homa.rating.domain.Party;
import com.homa.rating.domain.Rating;
import com.homa.rating.domain.RatingSession;
import com.homa.rating.domain.ReservationId;
import com.homa.rating.usecase.exception.RatingSessionChangedException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Stores the rating sessions, one per Completed reservation. */
public interface RatingSessionRepository {

    Optional<RatingSession> findByReservationId(ReservationId reservationId);

    /**
     * The reservations whose session is still OPEN and whose Rating Window closed at or before {@code now}, earliest
     * closing first. A query shortcut only: each session is loaded and asked again (RTG-3).
     */
    List<ReservationId> findIdsOfOpenWithWindowClosedBy(Instant now);

    /**
     * The revealed ratings that stand about {@code subject}: for each session where the subject is the customer or
     * the provider, the rating {@link RatingSession#revealedRatingAbout(Party)} returns (RTG-6).
     */
    List<Rating> findRevealedRatingsAbout(Party subject);

    /**
     * Inserts a new session or stores the changes to a loaded one, with its ratings. The session was loaded at its
     * {@code version()}, or is new at version 0; if someone else saved it since, or a new one already exists for the
     * reservation, nothing is saved and {@link RatingSessionChangedException} is thrown.
     */
    void save(RatingSession session);
}
