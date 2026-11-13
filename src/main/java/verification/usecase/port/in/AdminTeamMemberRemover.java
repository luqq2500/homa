package verification.usecase.port.in;

import verification.usecase.dto.RemoveTeamMemberByHomaRequest;
import verification.usecase.dto.RemoveTeamMemberByHomaResponse;

/** An admin removes a team member Homa learned is not allowed to work, and records why. */
public interface AdminTeamMemberRemover {

    RemoveTeamMemberByHomaResponse remove(RemoveTeamMemberByHomaRequest request);
}
