package verification.usecase.dto;

import java.time.Instant;

public record ReportTeamMemberReverificationNeededRequest(String memberId, Instant neededBy) {}
