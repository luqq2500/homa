package com.homa.rating.usecase;

import com.homa.rating.domain.Party;
import com.homa.rating.domain.PartyRole;
import com.homa.rating.domain.RatingCategory;
import com.homa.rating.domain.RatingSummary;
import com.homa.rating.domain.ReliabilityCounts;
import com.homa.rating.domain.ReliabilityRecord;
import com.homa.rating.usecase.dto.ViewRatingsAndReliabilityRequest;
import com.homa.rating.usecase.dto.ViewRatingsAndReliabilityResponse;
import com.homa.rating.usecase.exception.NotAllowedException;
import com.homa.rating.usecase.port.in.RatingsAndReliabilityViewer;
import com.homa.rating.usecase.port.out.RatingSessionRepository;
import com.homa.rating.usecase.port.out.ReliabilityRecordRepository;
import com.homa.rating.usecase.port.out.TimeSource;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RTG-6, RTG-10: what a party's page shows. The revealed ratings about them, with their average and the average on
 * each category, and the reliability counts over the last 12 months. A provider's are seen by it and by customers;
 * a customer's by them and by providers. This is also what Listing's provider page is built from, through the events.
 */
public final class ViewRatingsAndReliability implements RatingsAndReliabilityViewer {

    private final RatingSessionRepository sessions;
    private final ReliabilityRecordRepository reliability;
    private final TimeSource time;

    public ViewRatingsAndReliability(RatingSessionRepository sessions, ReliabilityRecordRepository reliability,
            TimeSource time) {
        this.sessions = sessions;
        this.reliability = reliability;
        this.time = time;
    }

    @Override
    public ViewRatingsAndReliabilityResponse view(ViewRatingsAndReliabilityRequest request) {
        Party viewer = new Party(PartyRole.valueOf(request.viewerRole()), request.viewerId());
        Party subject = new Party(PartyRole.valueOf(request.subjectRole()), request.subjectId());
        if (!subject.canBeSeenBy(viewer)) {
            throw new NotAllowedException(viewer.role() + " " + viewer.id() + " may not see the ratings and "
                    + "reliability of " + subject.role() + " " + subject.id());
        }
        RatingSummary summary = RatingSummary.of(sessions.findRevealedRatingsAbout(subject));
        ReliabilityCounts counts = reliability.findByParty(subject).orElseGet(() -> ReliabilityRecord.openFor(subject))
                .countsAt(time.now());
        return new ViewRatingsAndReliabilityResponse(subject.id(), subject.role().name(), summary.ratingCount(),
                summary.averageStars().orElse(null), categoryAverages(summary), counts.cancellations(),
                counts.noRosters(), counts.noShows(), counts.membersLeft());
    }

    private static Map<String, BigDecimal> categoryAverages(RatingSummary summary) {
        Map<String, BigDecimal> averages = new LinkedHashMap<>();
        for (RatingCategory category : RatingCategory.values()) {
            summary.averageOf(category).ifPresent(average -> averages.put(category.name(), average));
        }
        return averages;
    }
}
