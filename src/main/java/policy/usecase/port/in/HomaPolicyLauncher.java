package policy.usecase.port.in;

import policy.usecase.dto.LaunchPolicyRequest;
import policy.usecase.dto.LaunchPolicyResponse;

/** Homa's founders put the launch values in force, as version 1. */
public interface HomaPolicyLauncher {

    LaunchPolicyResponse launch(LaunchPolicyRequest request);
}
