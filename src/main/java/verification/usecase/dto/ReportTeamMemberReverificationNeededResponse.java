package verification.usecase.dto;

import java.time.Instant;

public record ReportTeamMemberReverificationNeededResponse(Instant verifiedUntil) {}
