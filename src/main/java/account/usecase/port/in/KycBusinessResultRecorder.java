package account.usecase.port.in;

import account.usecase.dto.RecordKybResultRequest;

/** The KYC provider reports a business's KYB or re-verification result. */
public interface KycBusinessResultRecorder {

    void record(RecordKybResultRequest request);
}
