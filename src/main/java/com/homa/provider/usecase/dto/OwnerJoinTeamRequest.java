package com.homa.provider.usecase.dto;

/**
 * The owner also works in their own team as a helper or supervisor (PRV-6).
 *
 * @param actingPersonId the person acting, who must be the provider's owner and is the one who joins
 * @param role           HELPER or SUPERVISOR
 */
public record OwnerJoinTeamRequest(String actingPersonId, String providerId, String role) {
}
