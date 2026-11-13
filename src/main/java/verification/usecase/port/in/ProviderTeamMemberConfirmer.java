package verification.usecase.port.in;

import verification.usecase.dto.ConfirmTeamMemberRequest;
import verification.usecase.dto.ConfirmTeamMemberResponse;

/** A provider confirms a verified person is lawfully employed; the person joins the team. */
public interface ProviderTeamMemberConfirmer {

    ConfirmTeamMemberResponse confirm(ConfirmTeamMemberRequest request);
}
