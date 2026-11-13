package offering.usecase.port.in;

import offering.usecase.dto.ChooseAcceptancePolicyRequest;
import offering.usecase.dto.ChooseAcceptancePolicyResponse;

/** A provider chooses Request and accept or Instant book. */
public interface ProviderAcceptancePolicyChooser {

    ChooseAcceptancePolicyResponse choose(ChooseAcceptancePolicyRequest request);
}
