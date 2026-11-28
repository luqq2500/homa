package com.homa.provider.usecase.dto;

/**
 * The member just removed. They return only as a new member, verified again (HLP-8).
 *
 * @param providerBookable false when this left the provider without an active helper or otherwise not bookable, so
 *                         it no longer shows in search (PRV-2)
 */
public record RemoveTeamMemberResponse(
        String memberId, String providerId, String personId, boolean providerBookable) {
}
