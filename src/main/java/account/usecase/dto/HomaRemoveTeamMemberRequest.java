package account.usecase.dto;

/** The founder, the team member and why. */
public record HomaRemoveTeamMemberRequest(String decidedBy, String memberId, String reason) {
}
