package verification.usecase.port.in;

import verification.usecase.dto.ChangeTeamMemberRoleRequest;
import verification.usecase.dto.ChangeTeamMemberRoleResponse;

/** A provider changes a team member between helper and supervisor. */
public interface ProviderTeamMemberRoleChanger {

    ChangeTeamMemberRoleResponse change(ChangeTeamMemberRoleRequest request);
}
