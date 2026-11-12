package policy.usecase;

import java.time.Clock;
import java.time.Instant;
import policy.domain.HomaPolicy;
import policy.usecase.dto.LaunchPolicyRequest;
import policy.usecase.dto.LaunchPolicyResponse;
import policy.usecase.exception.PolicyAlreadyLaunchedException;
import policy.usecase.port.in.HomaPolicyLauncher;
import policy.usecase.port.out.AdminAuditLog;
import policy.usecase.port.out.PolicyIEPublisher;
import policy.usecase.port.out.PolicyRepository;

/** Puts the launch values in force as version 1 and tells the other contexts. It is done once. */
public final class HomaLaunchPolicy implements HomaPolicyLauncher {

    private final PolicyRepository policies;
    private final PolicyIEPublisher events;
    private final AdminActions admin;
    private final Clock clock;

    public HomaLaunchPolicy(PolicyRepository policies, PolicyIEPublisher events, AdminAuditLog auditLog, Clock clock) {
        this.policies = policies;
        this.events = events;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public LaunchPolicyResponse launch(LaunchPolicyRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        policies.findCurrent().ifPresent(inForce -> {
            throw new PolicyAlreadyLaunchedException(inForce.version());
        });
        Instant now = clock.instant();

        HomaPolicy launched = HomaPolicy.initial(now);

        policies.save(launched);
        events.publish(PolicyChanged.from(launched.snapshot()));
        admin.record(founder, "LAUNCH_POLICY", "version " + launched.version(), now);
        return new LaunchPolicyResponse(launched.version(), launched.snapshot().effectiveFrom());
    }
}
