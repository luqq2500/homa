package catalogue.usecase;

import catalogue.usecase.exception.AdminReasonRequiredException;
import catalogue.usecase.exception.DecidedByRequiredException;
import catalogue.usecase.port.out.AdminAction;
import catalogue.usecase.port.out.AdminAuditLog;
import java.time.Instant;

/** What every admin usecase of the catalogue does the same way: it needs the founder, and it records the action. */
final class AdminActions {

    private static final String NO_REASON = "";

    private final AdminAuditLog auditLog;

    AdminActions(AdminAuditLog auditLog) {
        this.auditLog = auditLog;
    }

    /** The founder's id, which every admin action needs before anything is done. */
    static String founder(String decidedBy) {
        if (decidedBy == null || decidedBy.isBlank()) {
            throw new DecidedByRequiredException();
        }
        return decidedBy.strip();
    }

    /** The reason, for an action that needs one before anything is done. */
    static String reason(String reason, String action) {
        if (reason == null || reason.isBlank()) {
            throw new AdminReasonRequiredException(action);
        }
        return reason.strip();
    }

    void record(String founder, String action, String subject, Instant at) {
        record(founder, action, subject, NO_REASON, at);
    }

    void record(String founder, String action, String subject, String reason, Instant at) {
        auditLog.record(new AdminAction(founder, action, subject, reason, at));
    }
}
