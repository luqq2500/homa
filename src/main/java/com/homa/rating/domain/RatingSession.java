package com.homa.rating.domain;

import com.homa.rating.domain.exception.RatingAlreadySubmittedException;
import com.homa.rating.domain.exception.RatingAuthorNotAPartyException;
import com.homa.rating.domain.exception.RatingNotFoundException;
import com.homa.rating.domain.exception.RatingSessionNotOpenException;
import com.homa.rating.domain.exception.RatingWindowClosedException;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The ratings of one reservation: the customer's of the provider and the provider's of the customer (RTG-1). A
 * session exists only for a reservation whose job was marked Completed (RTG-9); it opens then and closes 7 days after
 * the Protection Window ends (RTG-2). Each side rates once, with direction-specific categories and media only from the
 * customer (RTG-4, RTG-5, RTG-7, RTG-8, enforced by {@link Rating}).
 *
 * <p>Ratings are double-blind: they are hidden until the moment both are in or the window closes, whichever comes
 * first, and then both show (RTG-3). A confirmed Provider No Show voids the session and removes its ratings, even
 * revealed ones (NSH-6). Support can remove one rating (RTG-11).
 *
 * <p>Whoever saves a session first moves its {@code version} on, and a save from an older version is refused, so two
 * ratings written together, or a rating and the window closing, never overwrite each other.
 */
public final class RatingSession {

    private final ReservationId reservationId;
    private final CustomerId customerId;
    private final ProviderId providerId;
    private final RatingWindow window;
    private final Map<PartyRole, Rating> ratings = new EnumMap<>(PartyRole.class);
    private RatingSessionStatus status = RatingSessionStatus.OPEN;
    private Instant revealedAt;
    private final long version;

    private RatingSession(ReservationId reservationId, CustomerId customerId, ProviderId providerId,
            RatingWindow window, long version) {
        this.reservationId = Objects.requireNonNull(reservationId, "reservationId");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.window = Objects.requireNonNull(window, "window");
        this.version = version;
    }

    /**
     * The job was marked Completed at {@code completedAt}: ratings open (RTG-2, RTG-9). The window closes 7 days
     * after {@code protectionWindowEnd}, by the value version in force when the reservation was Requested at
     * {@code requestedAt} (P3). A job marked Completed after the window would have closed gets no ratings, so there
     * is no session and {@link RatingWindowClosedException} is thrown.
     */
    public static RatingSession open(ReservationId reservationId, CustomerId customerId, ProviderId providerId,
            Instant completedAt, Instant protectionWindowEnd, Instant requestedAt) {
        return open(reservationId, customerId, providerId, completedAt, protectionWindowEnd, requestedAt,
                RatingValues.homa());
    }

    static RatingSession open(ReservationId reservationId, CustomerId customerId, ProviderId providerId,
            Instant completedAt, Instant protectionWindowEnd, Instant requestedAt, RatingValues values) {
        RatingWindow window = RatingWindow.opening(completedAt, protectionWindowEnd, values.inForceAt(requestedAt));
        return new RatingSession(reservationId, customerId, providerId, window, 0);
    }

    /**
     * Rebuilds a session from what was stored, with the {@code version} it was saved at. {@code revealedAt} is
     * null until revealed. Only the shape is checked: each rating is by its own side's party, each side has at most
     * one, an open session has nothing revealed and at most one rating, a revealed one has its moment, and a void one
     * holds no rating. Whether a rating was in time isn't checked again: it was when it was written.
     */
    public static RatingSession restore(ReservationId reservationId, CustomerId customerId, ProviderId providerId,
            RatingWindow window, List<Rating> ratings, RatingSessionStatus status, Instant revealedAt, long version) {
        RatingSession session = new RatingSession(reservationId, customerId, providerId, window, version);
        session.status = Objects.requireNonNull(status, "status");
        session.revealedAt = revealedAt;
        for (Rating rating : ratings) {
            requireShape(session.partyOf(rating.author().role()).equals(rating.author()),
                    "each rating by the party of its own side");
            requireShape(session.ratings.put(rating.author().role(), rating) == null, "one rating per side");
        }
        session.requireStoredShape();
        return session;
    }

    private void requireStoredShape() {
        switch (status) {
            case OPEN -> requireShape(revealedAt == null && ratings.size() < PartyRole.values().length,
                    "nothing revealed and at most one rating while still open");
            case REVEALED -> requireShape(revealedAt != null, "a reveal moment once revealed");
            case VOID -> requireShape(ratings.isEmpty(), "no ratings once void");
        }
    }

    private static void requireShape(boolean fits, String what) {
        if (!fits) {
            throw new IllegalArgumentException("A stored rating session needs " + what);
        }
    }

    /** The customer or the provider of this reservation, if {@code actorId} is one of them. */
    public Optional<PartyRole> roleOf(String actorId) {
        if (customerId.value().equals(actorId)) {
            return Optional.of(PartyRole.CUSTOMER);
        }
        if (providerId.value().equals(actorId)) {
            return Optional.of(PartyRole.PROVIDER);
        }
        return Optional.empty();
    }

    /** The party on one side of this reservation. */
    public Party partyOf(PartyRole role) {
        return role == PartyRole.CUSTOMER ? Party.customer(customerId) : Party.provider(providerId);
    }

    /**
     * A party rates the other (RTG-1). The session must still be taking ratings, the window must still be open when
     * the rating is submitted (RTG-2), the author must be this reservation's customer or provider, and each side
     * rates once. When the second rating comes in, both are revealed at once (RTG-3).
     */
    public void submit(Rating rating) {
        Objects.requireNonNull(rating, "rating");
        requireTakingRatings(rating);
        PartyRole side = rating.author().role();
        if (ratings.containsKey(side)) {
            throw new RatingAlreadySubmittedException(reservationId, side);
        }
        ratings.put(side, rating);
        if (ratings.size() == PartyRole.values().length) {
            reveal(rating.submittedAt());
        }
    }

    private void requireTakingRatings(Rating rating) {
        if (status != RatingSessionStatus.OPEN) {
            throw new RatingSessionNotOpenException(reservationId, status);
        }
        if (window.hasClosedBy(rating.submittedAt())) {
            throw new RatingWindowClosedException(rating.submittedAt(), window.closesAt());
        }
        if (!partyOf(rating.author().role()).equals(rating.author())) {
            throw new RatingAuthorNotAPartyException(reservationId, rating.author());
        }
    }

    /**
     * The scheduler's check (RTG-3): once the window has closed, whatever was rated is revealed, even when only one
     * side rated or neither did. The reveal moment is the closing moment, however late the scheduler runs. Returns
     * false, changing nothing, while the window is still open or when the session was already revealed or voided.
     */
    public boolean revealIfWindowClosed(Instant now) {
        if (status != RatingSessionStatus.OPEN || !window.hasClosedBy(now)) {
            return false;
        }
        reveal(window.closesAt());
        return true;
    }

    private void reveal(Instant at) {
        status = RatingSessionStatus.REVEALED;
        revealedAt = at;
    }

    /**
     * A Provider No Show was confirmed after the job was marked Completed: the session is void and its ratings are
     * removed, revealed or not (NSH-6). Returns false, changing nothing, when it was already void.
     */
    public boolean voidForProviderNoShow() {
        if (status == RatingSessionStatus.VOID) {
            return false;
        }
        ratings.clear();
        status = RatingSessionStatus.VOID;
        return true;
    }

    /**
     * Support removes a rating with abusive content or personal contact or address details (RTG-11), and returns it.
     * Removing it changes nothing else: a revealed session stays revealed, and a side that hasn't been revealed yet
     * may rate again while the window is open.
     */
    public Rating remove(RatingId ratingId) {
        Rating removed = ratings.values().stream().filter(rating -> rating.id().equals(ratingId)).findFirst()
                .orElseThrow(() -> new RatingNotFoundException(reservationId, ratingId));
        ratings.remove(removed.author().role());
        return removed;
    }

    /** The rating shown about {@code subject}: the other side's rating, once revealed (RTG-3, RTG-6). */
    public Optional<Rating> revealedRatingAbout(Party subject) {
        if (status != RatingSessionStatus.REVEALED || !partyOf(subject.role()).equals(subject)) {
            return Optional.empty();
        }
        return Optional.ofNullable(ratings.get(otherSide(subject.role())));
    }

    /** True while a customer's revealed rating stands, which is what Listing shows about the provider (RTG-6). */
    public boolean showsRatingOfProvider() {
        return revealedRatingAbout(partyOf(PartyRole.PROVIDER)).isPresent();
    }

    private static PartyRole otherSide(PartyRole role) {
        return role == PartyRole.CUSTOMER ? PartyRole.PROVIDER : PartyRole.CUSTOMER;
    }

    public ReservationId reservationId() {
        return reservationId;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public RatingWindow window() {
        return window;
    }

    public RatingSessionStatus status() {
        return status;
    }

    public boolean isRevealed() {
        return status == RatingSessionStatus.REVEALED;
    }

    /** The moment both ratings were in or the window closed (RTG-3); empty until revealed. */
    public Optional<Instant> revealedAt() {
        return Optional.ofNullable(revealedAt);
    }

    /**
     * Every rating the session holds, revealed or not, customer's first: what the store keeps. What anyone may see
     * is {@link #revealedRatingAbout(Party)}.
     */
    public List<Rating> ratings() {
        return List.copyOf(ratings.values());
    }

    /** The rating one side wrote, if it has and it stands. For its author and Support, never the other side. */
    public Optional<Rating> ratingBy(PartyRole side) {
        return Optional.ofNullable(ratings.get(side));
    }

    /**
     * The version the session was loaded at, 0 for one not saved yet. Whoever saves it first moves it on, and a save
     * from an older version is refused as a conflict.
     */
    public long version() {
        return version;
    }
}
