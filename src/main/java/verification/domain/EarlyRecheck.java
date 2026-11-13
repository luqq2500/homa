package verification.domain;

import java.time.Instant;
import verification.domain.exception.ReasonMissingException;

/** Homa's request to the KYC provider for an early re-verification, for example after a safety report. */
public record EarlyRecheck(String id, String subjectId, String reason, String adminId, Instant requestedAt) {

    public EarlyRecheck {
        if (reason == null || reason.isBlank()) {
            throw new ReasonMissingException();
        }
    }

    public static EarlyRecheck request(String subjectId, String reason, String adminId, Instant now) {
        return new EarlyRecheck(new EarlyRecheckId().generate(), subjectId, reason, adminId, now);
    }
}
