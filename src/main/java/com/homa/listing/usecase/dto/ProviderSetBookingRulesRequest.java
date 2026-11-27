package com.homa.listing.usecase.dto;

/**
 * The provider's booking rules: hours and helpers (OFR-5), buffer (OFR-10), acceptance policy and cancellation
 * tier (OFR-9), and minimum notice (OFR-6).
 *
 * @param maxHelpers           null keeps the default: up to the number of active helpers (OFR-5)
 * @param minimumNoticeMinutes null keeps the current notice, raised to 2 hours when switching to Request (OFR-6)
 * @param acceptancePolicy     INSTANT or REQUEST
 * @param cancellationTier     FLEXIBLE, STANDARD or FIRM
 */
public record ProviderSetBookingRulesRequest(
        String actingPersonId,
        String providerId,
        long minBookingMinutes,
        long maxBookingMinutes,
        int minHelpers,
        Integer maxHelpers,
        Long minimumNoticeMinutes,
        long jobBufferMinutes,
        String acceptancePolicy,
        String cancellationTier) {
}
