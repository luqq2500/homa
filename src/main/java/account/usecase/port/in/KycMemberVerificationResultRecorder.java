package account.usecase.port.in;

import account.usecase.dto.RecordMemberVerificationResultRequest;

/** The KYC provider reports the on-the-spot check of a person being added. */
public interface KycMemberVerificationResultRecorder {

    void record(RecordMemberVerificationResultRequest request);
}
