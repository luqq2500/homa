package account.usecase.port.in;

import account.usecase.dto.RecordMemberLapseRequest;

/** The KYC provider reports that a team member missed their re-verification. */
public interface KycMemberLapseRecorder {

    void record(RecordMemberLapseRequest request);
}
