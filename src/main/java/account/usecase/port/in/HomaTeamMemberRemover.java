package account.usecase.port.in;

import account.usecase.dto.HomaRemoveTeamMemberRequest;

/** Homa's founders remove a team member who may not work. */
public interface HomaTeamMemberRemover {

    void remove(HomaRemoveTeamMemberRequest request);
}
