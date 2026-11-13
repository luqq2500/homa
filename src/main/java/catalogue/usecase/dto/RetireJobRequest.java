package catalogue.usecase.dto;

public record RetireJobRequest(String adminId, String jobId, String reason) {}
