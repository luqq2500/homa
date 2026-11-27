package com.homa.provider.usecase.dto;

/**
 * The provider just opened.
 *
 * @param status OPENED: it builds its team and adds its payout account before going live
 */
public record OpenProviderResponse(String providerId, String ownerPersonId, String status) {
}
