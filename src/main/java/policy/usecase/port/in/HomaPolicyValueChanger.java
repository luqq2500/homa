package policy.usecase.port.in;

import policy.usecase.dto.ChangePolicyValueRequest;
import policy.usecase.dto.ChangePolicyValueResponse;

/** Homa's founders change one Homa-set value, which makes a new version for new reservations. */
public interface HomaPolicyValueChanger {

    ChangePolicyValueResponse change(ChangePolicyValueRequest request);
}
