package policy.usecase.port.in;

import policy.usecase.dto.HomaViewPolicyResponse;

/** Homa's founders view the Homa-set values in force. */
public interface HomaPolicyViewer {

    HomaViewPolicyResponse view();
}
