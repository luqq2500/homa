package com.homa.ie.events;

import com.homa.ie.port.ListingIE;
import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A full snapshot of a provider's listing terms, published on any change (OFR-1 to OFR-6, OFR-9, OFR-10).
 * Reservation keeps it in its BookableProvider replica and freezes it into ReservationTerms when a reservation
 * is Requested (RES-8); existing reservations keep their own terms.
 *
 * @param travelFeeKind  NONE, FLAT or BY_CITY (OFR-3)
 * @param flatTravelFee  the fee when travelFeeKind is FLAT, otherwise null
 * @param travelFeeByCity the fee per covered city when travelFeeKind is BY_CITY, otherwise empty
 * @param discountRate   null when the listing has no discount (OFR-4)
 * @param acceptancePolicy INSTANT or REQUEST
 * @param cancellationTier FLEXIBLE, STANDARD or FIRM
 * @param noticeRaised   true when switching to Request raised the minimum notice to 2 hours (OFR-6)
 */
public record ListingTermsChangedIE(
        String providerId,
        List<String> cities,
        List<String> jobCodes,
        Money hourlyRatePerHelper,
        String travelFeeKind,
        Money flatTravelFee,
        Map<String, Money> travelFeeByCity,
        Percentage discountRate,
        Duration discountMinHours,
        int discountMinHelpers,
        Duration minBookingHours,
        Duration maxBookingHours,
        int minHelpers,
        int maxHelpers,
        Duration minimumNotice,
        Duration jobBuffer,
        String acceptancePolicy,
        String cancellationTier,
        boolean noticeRaised,
        Instant occurredAt) implements ListingIE {

    public ListingTermsChangedIE {
        cities = List.copyOf(cities);
        jobCodes = List.copyOf(jobCodes);
        travelFeeByCity = Map.copyOf(travelFeeByCity);
    }
}
