package account.usecase.dto;

/**
 * The start of adding a team member, with the person beside the provider.
 *
 * @param providerId the business, from authentication
 * @param role {@code HELPER} or {@code SUPERVISOR}
 * @param photo the reference of the profile photo taken with the phone's camera
 */
public record StartAddingTeamMemberRequest(String providerId, String role, String photo) {
}
