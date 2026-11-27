package com.homa.listing.usecase.dto;

import com.homa.kernel.Money;
import java.math.BigDecimal;
import java.util.Map;

/**
 * The rate, travel fee and discount together (OFR-2 to OFR-4), checked together against Homa's minimum rate
 * (OFR-11).
 *
 * @param travelFeeKind      NONE, FLAT or BY_CITY
 * @param flatTravelFee      the fee when travelFeeKind is FLAT; ignored otherwise
 * @param travelFeeByCity    the fee per city code when travelFeeKind is BY_CITY; ignored otherwise
 * @param discountPercent    the discount rate, such as 10; null for no discount
 * @param discountMinMinutes the hours a reservation needs for the discount, in minutes; ignored without a discount
 * @param discountMinHelpers the helpers a reservation needs for the discount; ignored without a discount
 */
public record ProviderSetPricingTermsRequest(
        String actingPersonId,
        String providerId,
        Money hourlyRatePerHelper,
        String travelFeeKind,
        Money flatTravelFee,
        Map<String, Money> travelFeeByCity,
        BigDecimal discountPercent,
        long discountMinMinutes,
        int discountMinHelpers) {
}
