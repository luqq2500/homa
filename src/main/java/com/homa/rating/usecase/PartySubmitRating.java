package com.homa.rating.usecase;

import com.homa.rating.domain.Party;
import com.homa.rating.domain.PartyRole;
import com.homa.rating.domain.Rating;
import com.homa.rating.domain.RatingCategory;
import com.homa.rating.domain.RatingId;
import com.homa.rating.domain.RatingSession;
import com.homa.rating.domain.ReservationId;
import com.homa.rating.domain.Stars;
import com.homa.rating.usecase.dto.SubmitRatingRequest;
import com.homa.rating.usecase.dto.SubmitRatingResponse;
import com.homa.rating.usecase.exception.NotAllowedException;
import com.homa.rating.usecase.exception.RatingSessionNotFoundException;
import com.homa.rating.usecase.port.in.RatingSubmitter;
import com.homa.rating.usecase.port.out.RatingSessionRepository;
import com.homa.rating.usecase.port.out.TimeSource;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/**
 * RTG-1: after the job was marked Completed, the customer rates the provider and the provider rates the customer,
 * once each, while the Rating Window is open (RTG-2). The stars, the optional description and category stars, and the
 * media are the domain's to check (RTG-4, RTG-5, RTG-7, RTG-8). When this rating completes the pair, both are
 * revealed at once (RTG-3), and Listing is told if a customer's rating of the provider is now shown.
 */
public final class PartySubmitRating implements RatingSubmitter {

    private final RatingSessionRepository sessions;
    private final ProviderRatingUpdates providerRatings;
    private final TimeSource time;

    public PartySubmitRating(RatingSessionRepository sessions, ProviderRatingUpdates providerRatings,
            TimeSource time) {
        this.sessions = sessions;
        this.providerRatings = providerRatings;
        this.time = time;
    }

    @Override
    public SubmitRatingResponse submit(SubmitRatingRequest request) {
        Instant now = time.now();
        RatingSession session = sessions.findByReservationId(new ReservationId(request.reservationId()))
                .orElseThrow(() -> new RatingSessionNotFoundException(request.reservationId()));
        PartyRole role = session.roleOf(request.actorId()).orElseThrow(() -> new NotAllowedException(
                request.actorId() + " is not a party of reservation " + request.reservationId()
                        + ", so can't rate it"));
        Party author = session.partyOf(role);
        Rating rating = Rating.by(RatingId.generate(), author, new Stars(request.stars()), request.description(),
                categoryStars(request.categoryStars()), request.mediaRefs(), now);

        boolean shownBefore = session.showsRatingOfProvider();
        session.submit(rating);
        sessions.save(session);
        providerRatings.publishWhenChanged(shownBefore, session);

        return new SubmitRatingResponse(request.reservationId(), rating.id().value(), role.name(),
                session.isRevealed(), session.revealedAt().orElse(null));
    }

    private static Map<RatingCategory, Stars> categoryStars(Map<String, Integer> given) {
        Map<RatingCategory, Stars> categories = new EnumMap<>(RatingCategory.class);
        given.forEach((name, stars) -> categories.put(RatingCategory.valueOf(name), new Stars(stars)));
        return categories;
    }
}
