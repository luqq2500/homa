package com.homa.provider.usecase.dto;

/**
 * The owner takes the provider live (PRV-2, PRV-3).
 *
 * @param actingPersonId the person acting, who must be the provider's owner
 */
public record GoLiveRequest(String actingPersonId, String providerId) {
}
