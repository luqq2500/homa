package offering.usecase.port.in;

import offering.usecase.dto.SetSupervisorRuleRequest;
import offering.usecase.dto.SetSupervisorRuleResponse;

/** A provider sets when a supervisor is sent: from a number of helpers, hours, both, or (0 and 0) never. */
public interface ProviderSupervisorRuleSetter {

    SetSupervisorRuleResponse set(SetSupervisorRuleRequest request);
}
