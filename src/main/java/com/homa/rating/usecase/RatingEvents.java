package com.homa.rating.usecase;

import com.homa.ie.events.ProviderRatingRevealedIE;
import com.homa.ie.events.ReliabilityChangedIE;
import com.homa.rating.domain.RatingCategory;
import com.homa.rating.domain.RatingSummary;
import com.homa.rating.domain.ReliabilityCounts;
import com.homa.rating.domain.ReliabilityRecord;
import com.homa.rating.domain.ProviderId;
import com.homa.rating.usecase.port.out.EventIds;
import com.homa.rating.usecase.port.out.TimeSource;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Builds the Rating context's integration events from the domain's answers, field by field, with a new event id and
 * the moment of publishing. Every number it carries is the domain's own, never computed here.
 */
public final class RatingEvents {

    private final EventIds eventIds;
    private final TimeSource time;

    public RatingEvents(EventIds eventIds, TimeSource time) {
        this.eventIds = eventIds;
        this.time = time;
    }

    /** RTG-3, RTG-6: what customers have said about the provider, as it stands now. */
    public ProviderRatingRevealedIE providerRatingRevealed(ProviderId providerId, RatingSummary summary) {
        return new ProviderRatingRevealedIE(eventIds.next(), providerId.value(), summary.ratingCount(),
                text(summary.averageStars()), text(summary.averageOf(RatingCategory.QUALITY_OF_WORK)),
                text(summary.averageOf(RatingCategory.PUNCTUALITY)),
                text(summary.averageOf(RatingCategory.PROFESSIONALISM)), time.now());
    }

    /** RTG-10: the party's counts over the last 12 months, after an incident was logged. */
    public ReliabilityChangedIE reliabilityChanged(ReliabilityRecord record) {
        ReliabilityCounts counts = record.countsAt(time.now());
        return new ReliabilityChangedIE(eventIds.next(), record.party().id(), record.party().role().name(),
                counts.cancellations(), counts.noRosters(), counts.noShows(), counts.membersLeft(), time.now());
    }

    private static String text(Optional<BigDecimal> average) {
        return average.map(BigDecimal::toPlainString).orElse(null);
    }
}
