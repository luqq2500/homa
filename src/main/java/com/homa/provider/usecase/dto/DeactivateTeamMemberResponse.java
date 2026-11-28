package com.homa.provider.usecase.dto;

/**
 * The member just deactivated.
 *
 * @param providerBookable false when this left the provider without an active helper or otherwise not bookable, so
 *                         it no longer shows in search (PRV-2)
 */
public record DeactivateTeamMemberResponse(
        String memberId, String providerId, String personId, boolean providerBookable) {
}
