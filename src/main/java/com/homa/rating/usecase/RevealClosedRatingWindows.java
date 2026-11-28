package com.homa.rating.usecase;

import com.homa.rating.domain.RatingSession;
import com.homa.rating.domain.ReservationId;
import com.homa.rating.usecase.dto.RevealClosedRatingWindowsRequest;
import com.homa.rating.usecase.dto.RevealClosedRatingWindowsResponse;
import com.homa.rating.usecase.exception.RatingSessionChangedException;
import com.homa.rating.usecase.port.in.ClosedWindowRevealer;
import com.homa.rating.usecase.port.out.RatingSessionRepository;
import com.homa.rating.usecase.port.out.TimeSource;
import com.homa.rating.usecase.port.out.UnitOfWork;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RTG-3, run by a scheduler: every session whose Rating Window has closed is revealed, with whatever ratings it
 * holds, even when only one side rated or neither did. Each session is loaded and revealed in a unit of work of its
 * own, so one session that fails or was changed by someone else never undoes the others, and two runs at once can't
 * reveal the same session twice: the later one finds it revealed, or loses the save and leaves it alone.
 * Publishes the provider's ratings when a customer's rating of it is revealed (RTG-6).
 */
public final class RevealClosedRatingWindows implements ClosedWindowRevealer {

    private final RatingSessionRepository sessions;
    private final ProviderRatingUpdates providerRatings;
    private final UnitOfWork unitOfWork;
    private final TimeSource time;

    public RevealClosedRatingWindows(RatingSessionRepository sessions, ProviderRatingUpdates providerRatings,
            UnitOfWork unitOfWork, TimeSource time) {
        this.sessions = sessions;
        this.providerRatings = providerRatings;
        this.unitOfWork = unitOfWork;
        this.time = time;
    }

    @Override
    public RevealClosedRatingWindowsResponse revealClosed(RevealClosedRatingWindowsRequest request) {
        Instant now = time.now();
        List<String> revealed = new ArrayList<>();
        List<String> changedMeanwhile = new ArrayList<>();
        for (ReservationId reservationId : sessions.findIdsOfOpenWithWindowClosedBy(now)) {
            try {
                if (unitOfWork.inOwnTransaction(() -> reveal(reservationId, now))) {
                    revealed.add(reservationId.value());
                }
            } catch (RatingSessionChangedException someoneElseSavedFirst) {
                changedMeanwhile.add(reservationId.value());
            }
        }
        return new RevealClosedRatingWindowsResponse(revealed, changedMeanwhile);
    }

    /** False, changing nothing, when the session is gone or is no longer waiting for its window to close. */
    private boolean reveal(ReservationId reservationId, Instant now) {
        Optional<RatingSession> found = sessions.findByReservationId(reservationId);
        if (found.isEmpty()) {
            return false;
        }
        RatingSession session = found.get();
        boolean shownBefore = session.showsRatingOfProvider();
        if (!session.revealIfWindowClosed(now)) {
            return false;
        }
        sessions.save(session);
        providerRatings.publishWhenChanged(shownBefore, session);
        return true;
    }
}
