package com.homa.provider.usecase.dto;

/**
 * The owner deactivates a member of the team, for example for two weeks' leave (HLP-5).
 *
 * @param actingPersonId the person acting, who must be the provider's owner
 * @param memberId       the member to deactivate, from the provider's team
 */
public record DeactivateTeamMemberRequest(String actingPersonId, String providerId, String memberId) {
}
