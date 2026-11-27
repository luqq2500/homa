package com.homa.provider.usecase.dto;

/**
 * The owner adds a verified person to the provider's team (HLP-1 to HLP-4).
 *
 * @param actingPersonId      the person acting, who must be the provider's owner
 * @param personId            the person to add, as Verification knows them
 * @param role                HELPER or SUPERVISOR
 * @param employmentConfirmed the provider confirms a foreign member works lawfully for it (HLP-4); ignored for a local
 *                            member
 */
public record AddTeamMemberRequest(
        String actingPersonId, String providerId, String personId, String role, boolean employmentConfirmed) {
}
