package policy.domain;

import java.time.Instant;
import policy.domain.exception.ChangeReasonMissingException;

/** The record of one value changed by an admin: what changed, why, who changed it and when. */
public record ValueChange(String id, HomaValue value, long from, long to, long version, String reason,
                          String adminId, Instant changedAt) {

    public ValueChange {
        if (reason == null || reason.isBlank()) {
            throw new ChangeReasonMissingException();
        }
    }

    /** Records the change that took {@code before} to {@code after}. */
    public static ValueChange record(HomaValues before, HomaValues after, HomaValue value, String reason,
                                     String adminId, Instant changedAt) {
        return new ValueChange(new ValueChangeId().generate(), value, before.amountOf(value), after.amountOf(value),
                after.version(), reason, adminId, changedAt);
    }
}
