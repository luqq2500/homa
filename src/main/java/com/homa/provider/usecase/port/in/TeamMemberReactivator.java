package com.homa.provider.usecase.port.in;

import com.homa.provider.usecase.dto.ReactivateTeamMemberRequest;
import com.homa.provider.usecase.dto.ReactivateTeamMemberResponse;

/** The owner reactivates a deactivated member who has a valid verification (HLP-5, HLP-7). */
public interface TeamMemberReactivator {

    ReactivateTeamMemberResponse reactivate(ReactivateTeamMemberRequest request);
}
