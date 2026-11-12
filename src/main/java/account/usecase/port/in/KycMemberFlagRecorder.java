package account.usecase.port.in;

import account.usecase.dto.RecordMemberFlagRequest;

/** The KYC provider's monitoring reports a confirmed serious flag on a team member. */
public interface KycMemberFlagRecorder {

    void record(RecordMemberFlagRequest request);
}
