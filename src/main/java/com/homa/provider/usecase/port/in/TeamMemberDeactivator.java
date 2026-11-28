package com.homa.provider.usecase.port.in;

import com.homa.provider.usecase.dto.DeactivateTeamMemberRequest;
import com.homa.provider.usecase.dto.DeactivateTeamMemberResponse;

/** The owner deactivates a member, who then can't be rostered (HLP-5, HLP-6). */
public interface TeamMemberDeactivator {

    DeactivateTeamMemberResponse deactivate(DeactivateTeamMemberRequest request);
}
