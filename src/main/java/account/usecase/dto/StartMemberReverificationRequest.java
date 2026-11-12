package account.usecase.dto;

/** The business, from authentication, and the team member to re-verify. */
public record StartMemberReverificationRequest(String providerId, String memberId) {
}
