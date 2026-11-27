package com.homa.listing.usecase.dto;

import com.homa.kernel.Money;
import java.util.List;

/**
 * A provider sets up its listing before going live, choosing what has no default: its jobs, tools and cities from
 * Homa's presets (OFR-1), its rate (OFR-2), its acceptance policy and its cancellation tier (OFR-9).
 *
 * @param actingPersonId   the person acting, who must be the provider's owner
 * @param acceptancePolicy INSTANT or REQUEST
 * @param cancellationTier FLEXIBLE, STANDARD or FIRM
 */
public record ProviderCreateListingRequest(
        String actingPersonId,
        String providerId,
        List<String> jobCodes,
        List<String> toolCodes,
        List<String> cityCodes,
        Money hourlyRatePerHelper,
        String acceptancePolicy,
        String cancellationTier) {
}
