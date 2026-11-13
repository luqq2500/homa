package verification.usecase.dto;

import java.time.Instant;

public record ReportBusinessReverificationNeededRequest(String businessId, Instant neededBy) {}
