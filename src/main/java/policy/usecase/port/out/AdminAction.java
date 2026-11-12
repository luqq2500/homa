package policy.usecase.port.out;

import java.time.Instant;
import java.util.Objects;

/**
 * One thing a founder did in the admin console.
 *
 * @param decidedBy the founder's id, from authentication
 * @param action what was done, such as {@code CHANGE_POLICY_VALUE}
 * @param subject what it was done to, such as {@code SERVICE_FEE=17.5%}
 * @param reason why, or empty when the action needs no reason
 * @param at when
 */
public record AdminAction(String decidedBy, String action, String subject, String reason, Instant at) {

    public AdminAction {
        Objects.requireNonNull(decidedBy, "decidedBy");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(at, "at");
    }
}
