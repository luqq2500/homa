package account.usecase;

import account.usecase.exception.AdminReasonRequiredException;
import account.usecase.exception.DecidedByRequiredException;
import account.usecase.port.out.AdminAction;
import account.usecase.port.out.AdminAuditLog;
import java.time.Instant;

/** What every admin usecase of the account context does the same way: it needs the founder and a reason, and it records the action. */
final class AdminActions {

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

    /** The reason, which every admin action about a person or a business needs before anything is done. */
    static String reason(String reason, String action) {
        if (reason == null || reason.isBlank()) {
            throw new AdminReasonRequiredException(action);
        }
        return reason.strip();
    }

    void record(String founder, String action, String subject, String reason, Instant at) {
        auditLog.record(new AdminAction(founder, action, subject, reason, at));
    }
}
