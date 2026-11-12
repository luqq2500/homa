package account.usecase.dto;

/** The business, from authentication, and the team member to remove. */
public record ProviderRemoveTeamMemberRequest(String providerId, String memberId) {
}
