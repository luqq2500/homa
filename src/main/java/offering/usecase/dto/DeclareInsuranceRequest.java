package offering.usecase.dto;

import java.time.Instant;

public record DeclareInsuranceRequest(String providerId, String insurer, Instant validUntil) {}
