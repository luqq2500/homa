package verification.usecase.dto;

import java.time.Instant;

public record ReportBusinessResultRequest(String businessId, boolean passed, String ssmNumber, String registeredName, Instant verifiedUntil) {}
