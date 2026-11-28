package com.homa.provider.usecase.dto;

/**
 * The member just reactivated.
 *
 * @param providerBookable true when the provider can be booked now (PRV-2)
 */
public record ReactivateTeamMemberResponse(
        String memberId, String providerId, String personId, boolean providerBookable) {
}
