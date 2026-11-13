package verification.usecase.dto;

import java.time.Instant;

public record ReportBusinessReverificationNeededResponse(Instant verifiedUntil) {}
