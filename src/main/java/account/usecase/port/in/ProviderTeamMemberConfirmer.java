package account.usecase.port.in;

import account.usecase.dto.ConfirmTeamMemberRequest;
import account.usecase.dto.ConfirmTeamMemberResponse;

/** A business confirms a verified, named person is lawfully employed or engaged, and they join the team. */
public interface ProviderTeamMemberConfirmer {

    ConfirmTeamMemberResponse confirm(ConfirmTeamMemberRequest request);
}
