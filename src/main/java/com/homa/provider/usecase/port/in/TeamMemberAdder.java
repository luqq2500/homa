package com.homa.provider.usecase.port.in;

import com.homa.provider.usecase.dto.AddTeamMemberRequest;
import com.homa.provider.usecase.dto.AddTeamMemberResponse;

/** The owner adds a verified person to the provider's team as a helper or supervisor (HLP-1 to HLP-4). */
public interface TeamMemberAdder {

    AddTeamMemberResponse add(AddTeamMemberRequest request);
}
