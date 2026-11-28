package com.homa.provider.usecase.port.in;

import com.homa.provider.usecase.dto.RemoveTeamMemberRequest;
import com.homa.provider.usecase.dto.RemoveTeamMemberResponse;

/** The owner removes a member, except while they're in a rostered window (HLP-8, HLP-9). */
public interface TeamMemberRemover {

    RemoveTeamMemberResponse remove(RemoveTeamMemberRequest request);
}
