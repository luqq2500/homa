package verification.usecase.port.in;

import verification.usecase.dto.RemoveTeamMemberRequest;
import verification.usecase.dto.RemoveTeamMemberResponse;

/** A provider removes a person from the team. */
public interface ProviderTeamMemberRemover {

    RemoveTeamMemberResponse remove(RemoveTeamMemberRequest request);
}
