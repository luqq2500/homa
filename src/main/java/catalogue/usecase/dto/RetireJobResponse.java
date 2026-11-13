package catalogue.usecase.dto;

import java.time.Instant;

public record RetireJobResponse(String jobId, Instant retiredAt) {}
