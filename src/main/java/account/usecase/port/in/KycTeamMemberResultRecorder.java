package account.usecase.port.in;

import account.usecase.dto.RecordTeamMemberResultRequest;

/** The KYC provider reports a team member's re-verification result. */
public interface KycTeamMemberResultRecorder {

    void record(RecordTeamMemberResultRequest request);
}
