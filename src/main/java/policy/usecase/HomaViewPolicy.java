package policy.usecase;

import java.util.Arrays;
import java.util.List;
import policy.domain.PolicySnapshot;
import policy.usecase.dto.HomaViewPolicyResponse;
import policy.usecase.exception.PolicyNotLaunchedException;
import policy.usecase.port.in.HomaPolicyViewer;
import policy.usecase.port.out.PolicyRepository;

/** Shows the founders every Homa-set value in force, by the name and in the form the change form takes. */
public final class HomaViewPolicy implements HomaPolicyViewer {

    private final PolicyRepository policies;

    public HomaViewPolicy(PolicyRepository policies) {
        this.policies = policies;
    }

    @Override
    public HomaViewPolicyResponse view() {
        PolicySnapshot inForce = policies.findCurrent().orElseThrow(PolicyNotLaunchedException::new).snapshot();
        List<HomaViewPolicyResponse.Value> values = Arrays.stream(PolicyValueName.values())
                .map(name -> new HomaViewPolicyResponse.Value(name.name(), name.kind().name(), name.amountIn(inForce)))
                .toList();
        return new HomaViewPolicyResponse(inForce.version(), inForce.effectiveFrom(), values);
    }
}
