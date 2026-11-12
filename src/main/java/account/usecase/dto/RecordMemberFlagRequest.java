package account.usecase.dto;

/** The team member and the flag, such as a sanctions match. */
public record RecordMemberFlagRequest(String memberId, String reason) {
}
