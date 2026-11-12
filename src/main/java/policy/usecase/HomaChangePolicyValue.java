package policy.usecase;

import java.time.Clock;
import java.time.Instant;
import policy.domain.HomaPolicy;
import policy.usecase.dto.ChangePolicyValueRequest;
import policy.usecase.dto.ChangePolicyValueResponse;
import policy.usecase.exception.PolicyNotLaunchedException;
import policy.usecase.port.in.HomaPolicyValueChanger;
import policy.usecase.port.out.AdminAuditLog;
import policy.usecase.port.out.PolicyIEPublisher;
import policy.usecase.port.out.PolicyRepository;

/**
 * Changes one Homa-set value, which makes a new version in force from now, and tells the other contexts.
 * Reservations already requested keep the version they were made with. A change applies to every new
 * reservation, so it needs a reason.
 */
public final class HomaChangePolicyValue implements HomaPolicyValueChanger {

    private final PolicyRepository policies;
    private final PolicyIEPublisher events;
    private final AdminActions admin;
    private final Clock clock;

    public HomaChangePolicyValue(PolicyRepository policies, PolicyIEPublisher events, AdminAuditLog auditLog, Clock clock) {
        this.policies = policies;
        this.events = events;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public ChangePolicyValueResponse change(ChangePolicyValueRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        String reason = AdminActions.reason(request.reason(), "change a Homa-set value");
        PolicyValueName name = PolicyValueName.named(request.value());
        HomaPolicy inForce = policies.findCurrent().orElseThrow(PolicyNotLaunchedException::new);
        Instant now = clock.instant();

        HomaPolicy next = inForce.change(name.toValue(request.amount()), now);
        String amount = name.amountIn(next.snapshot());

        policies.save(next);
        events.publish(PolicyChanged.from(next.snapshot()));
        admin.record(founder, "CHANGE_POLICY_VALUE", name.name() + "=" + amount, reason, now);
        return new ChangePolicyValueResponse(next.version(), next.snapshot().effectiveFrom(), name.name(), amount);
    }
}
