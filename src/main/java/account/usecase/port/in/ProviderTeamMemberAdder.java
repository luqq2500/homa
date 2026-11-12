package account.usecase.port.in;

import account.usecase.dto.StartAddingTeamMemberRequest;
import account.usecase.dto.StartAddingTeamMemberResponse;

/** A business starts adding a helper or a supervisor, who verifies on the spot. */
public interface ProviderTeamMemberAdder {

    StartAddingTeamMemberResponse start(StartAddingTeamMemberRequest request);
}
