package verification.usecase.dto;

public record ConfirmTeamMemberRequest(String businessId, String memberId, boolean lawfullyEmployed, boolean holdsValidWorkPass, String termsVersion) {}
