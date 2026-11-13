package verification.usecase.dto;

import java.time.Instant;

public record ReportTeamMemberResultRequest(String memberId, String result, String registeredName, String documentCountry, Instant verifiedUntil) {}
