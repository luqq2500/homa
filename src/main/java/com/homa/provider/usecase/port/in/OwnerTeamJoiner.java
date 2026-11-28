package com.homa.provider.usecase.port.in;

import com.homa.provider.usecase.dto.OwnerJoinTeamRequest;
import com.homa.provider.usecase.dto.OwnerJoinTeamResponse;

/** The owner also works in their own team as a helper or supervisor (PRV-6). */
public interface OwnerTeamJoiner {

    OwnerJoinTeamResponse join(OwnerJoinTeamRequest request);
}
