package verification.usecase.dto;

public record ChangeTeamMemberRoleRequest(String businessId, String memberId, String role) {}
