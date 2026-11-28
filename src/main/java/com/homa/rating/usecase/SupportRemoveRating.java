package com.homa.rating.usecase;

import com.homa.rating.domain.Rating;
import com.homa.rating.domain.RatingId;
import com.homa.rating.domain.RatingSession;
import com.homa.rating.domain.ReservationId;
import com.homa.rating.usecase.dto.RemoveRatingRequest;
import com.homa.rating.usecase.dto.RemoveRatingResponse;
import com.homa.rating.usecase.exception.NotAllowedException;
import com.homa.rating.usecase.exception.RatingSessionNotFoundException;
import com.homa.rating.usecase.port.in.RatingRemover;
import com.homa.rating.usecase.port.out.RatingSessionRepository;

/**
 * RTG-11: Support removes a rating with abusive content or personal contact or address details. Only the Support
 * role may. When the removed rating was a customer's revealed rating of the provider, Listing is told the new numbers.
 */
public final class SupportRemoveRating implements RatingRemover {

    /** The only role that may remove a rating. */
    static final String SUPPORT_ROLE = "SUPPORT";

    private final RatingSessionRepository sessions;
    private final ProviderRatingUpdates providerRatings;

    public SupportRemoveRating(RatingSessionRepository sessions, ProviderRatingUpdates providerRatings) {
        this.sessions = sessions;
        this.providerRatings = providerRatings;
    }

    @Override
    public RemoveRatingResponse remove(RemoveRatingRequest request) {
        if (!SUPPORT_ROLE.equals(request.actingRole())) {
            throw new NotAllowedException(request.actingPersonId() + " isn't Support, so can't remove a rating");
        }
        RatingSession session = sessions.findByReservationId(new ReservationId(request.reservationId()))
                .orElseThrow(() -> new RatingSessionNotFoundException(request.reservationId()));

        boolean shownBefore = session.showsRatingOfProvider();
        Rating removed = session.remove(new RatingId(request.ratingId()));
        sessions.save(session);
        providerRatings.publishWhenChanged(shownBefore, session);

        return new RemoveRatingResponse(request.reservationId(), request.ratingId(), removed.author().role().name());
    }
}
