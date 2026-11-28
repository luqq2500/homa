package com.homa.provider.usecase.dto;

/**
 * The owner removes a member of the team, as it must for one who no longer works for it (HLP-8).
 *
 * @param actingPersonId the person acting, who must be the provider's owner
 * @param memberId       the member to remove, from the provider's team
 */
public record RemoveTeamMemberRequest(String actingPersonId, String providerId, String memberId) {
}
