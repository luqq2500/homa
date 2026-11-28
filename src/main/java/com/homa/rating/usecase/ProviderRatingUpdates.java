package com.homa.rating.usecase;

import com.homa.rating.domain.PartyRole;
import com.homa.rating.domain.RatingSession;
import com.homa.rating.domain.RatingSummary;
import com.homa.rating.usecase.port.out.RatingIEPublisher;
import com.homa.rating.usecase.port.out.RatingSessionRepository;

/**
 * Tells Listing what customers say about a provider when a session changes what is shown about it (RTG-3, RTG-6,
 * RTG-11, NSH-6): a customer's rating was revealed, or one that was shown is removed or voided. A change to what is
 * shown about the customer, or to something still hidden, publishes nothing.
 */
public final class ProviderRatingUpdates {

    private final RatingSessionRepository sessions;
    private final RatingIEPublisher publisher;
    private final RatingEvents events;

    public ProviderRatingUpdates(RatingSessionRepository sessions, RatingIEPublisher publisher,
            RatingEvents events) {
        this.sessions = sessions;
        this.publisher = publisher;
        this.events = events;
    }

    /**
     * Publishes the provider's ratings as they now stand, when the saved {@code session} shows a customer's rating
     * of the provider and it didn't before ({@code shownBefore}), or the other way round.
     */
    public void publishWhenChanged(boolean shownBefore, RatingSession session) {
        if (shownBefore == session.showsRatingOfProvider()) {
            return;
        }
        RatingSummary summary = RatingSummary
                .of(sessions.findRevealedRatingsAbout(session.partyOf(PartyRole.PROVIDER)));
        publisher.publish(events.providerRatingRevealed(session.providerId(), summary));
    }
}
