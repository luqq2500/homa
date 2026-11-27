package com.homa.listing.usecase;

import com.homa.ie.events.AvailabilityChangedIE;
import com.homa.ie.events.ListingTermsChangedIE;
import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import com.homa.listing.domain.AvailabilitySchedule;
import com.homa.listing.domain.BookingBounds;
import com.homa.listing.domain.City;
import com.homa.listing.domain.DiscountRule;
import com.homa.listing.domain.JobType;
import com.homa.listing.domain.ListingTerms;
import com.homa.listing.domain.ListingTermsChange;
import com.homa.listing.domain.ProviderId;
import com.homa.listing.domain.ProviderStanding;
import com.homa.listing.domain.TravelFeePolicy;
import com.homa.listing.usecase.port.out.EventIds;
import com.homa.listing.usecase.port.out.ProviderStandingRepository;
import com.homa.listing.usecase.port.out.TimeSource;
import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds Listing's events from the saved aggregates, field by field. {@code ListingTermsChangedIE} carries the
 * maximum of helpers a customer can book now: the provider's own maximum, or its active helpers by default, and
 * never more than are active (OFR-5). Codes are sorted, so the same terms always give the same event.
 */
public final class ListingEvents {

    private final TimeSource time;
    private final EventIds eventIds;
    private final ProviderStandingRepository standings;

    public ListingEvents(TimeSource time, EventIds eventIds, ProviderStandingRepository standings) {
        this.time = time;
        this.eventIds = eventIds;
        this.standings = standings;
    }

    /** The full terms snapshot after a change, with the notice-raised flag (OFR-6). */
    public ListingTermsChangedIE termsChanged(ListingTermsChange change) {
        ListingTerms terms = change.terms();
        BookingBounds bounds = terms.bounds();
        TravelFeePolicy travelFee = terms.travelFee();
        DiscountRule discount = terms.discount();
        return new ListingTermsChangedIE(
                eventIds.next(),
                terms.providerId().value(),
                sortedCodes(terms.cities(), City::code),
                sortedCodes(terms.jobs(), JobType::code),
                terms.rate().perHelperPerHour(),
                TravelFeeKind.of(travelFee).name(),
                flatFee(travelFee),
                feesByCity(travelFee),
                discountRate(discount),
                discountMinHours(discount),
                discountMinHelpers(discount),
                bounds.minHours(),
                bounds.maxHours(),
                bounds.step(),
                bounds.minHelpers(),
                bounds.maxHelpersWith(activeHelperCount(terms.providerId())),
                terms.notice().duration(),
                terms.buffer().duration(),
                terms.policy().name(),
                terms.tier().name(),
                change.noticeRaised(),
                time.now());
    }

    /** The weekly hours now in force, each "DAY HH:MM-HH:MM", Monday first. */
    public AvailabilityChangedIE availabilityChanged(AvailabilitySchedule schedule) {
        return new AvailabilityChangedIE(eventIds.next(), schedule.providerId().value(),
                WeeklyHoursFormat.format(schedule.weeklyHours()), time.now());
    }

    private int activeHelperCount(ProviderId providerId) {
        return standings.findById(providerId).orElseGet(() -> ProviderStanding.unknown(providerId)).activeHelperCount();
    }

    private static <T> List<String> sortedCodes(Set<T> values, Function<T, String> code) {
        return values.stream().map(code).sorted().toList();
    }

    private static Money flatFee(TravelFeePolicy travelFee) {
        return travelFee instanceof TravelFeePolicy.Flat flat ? flat.fee() : null;
    }

    private static Map<String, Money> feesByCity(TravelFeePolicy travelFee) {
        return travelFee instanceof TravelFeePolicy.ByCity byCity
                ? byCity.fees().entrySet().stream().collect(Collectors.toMap(e -> e.getKey().code(), Map.Entry::getValue))
                : Map.of();
    }

    private static Percentage discountRate(DiscountRule discount) {
        return discount instanceof DiscountRule.Threshold threshold ? threshold.rate() : null;
    }

    private static Duration discountMinHours(DiscountRule discount) {
        return discount instanceof DiscountRule.Threshold threshold ? threshold.minHours() : null;
    }

    private static int discountMinHelpers(DiscountRule discount) {
        return discount instanceof DiscountRule.Threshold threshold ? threshold.minHelpers() : 0;
    }
}
