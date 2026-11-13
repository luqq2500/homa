package verification.usecase.dto;

public record RemoveTeamMemberByHomaRequest(String adminId, String memberId, String reason) {}
