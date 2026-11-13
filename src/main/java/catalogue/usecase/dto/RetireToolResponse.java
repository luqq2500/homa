package catalogue.usecase.dto;

import java.time.Instant;

public record RetireToolResponse(String toolId, Instant retiredAt) {}
