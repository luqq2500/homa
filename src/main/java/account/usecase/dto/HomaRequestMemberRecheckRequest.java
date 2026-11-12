package account.usecase.dto;

/** The founder, the team member and why, for example after a safety-related report. */
public record HomaRequestMemberRecheckRequest(String decidedBy, String memberId, String reason) {
}
