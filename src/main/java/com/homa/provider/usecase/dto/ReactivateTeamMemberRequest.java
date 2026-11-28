package com.homa.provider.usecase.dto;

/**
 * The owner reactivates a deactivated member, who must have a valid verification (HLP-5, HLP-7).
 *
 * @param actingPersonId the person acting, who must be the provider's owner
 * @param memberId       the member to reactivate, from the provider's team
 */
public record ReactivateTeamMemberRequest(String actingPersonId, String providerId, String memberId) {
}
