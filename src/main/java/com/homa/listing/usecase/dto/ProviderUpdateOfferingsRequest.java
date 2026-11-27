package com.homa.listing.usecase.dto;

import java.util.List;

/** The jobs, tools and cities the provider covers from now on, replacing the previous ones (OFR-1). */
public record ProviderUpdateOfferingsRequest(
        String actingPersonId, String providerId, List<String> jobCodes, List<String> toolCodes, List<String> cityCodes) {
}
