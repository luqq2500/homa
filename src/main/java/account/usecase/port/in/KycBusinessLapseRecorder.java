package account.usecase.port.in;

import account.usecase.dto.RecordBusinessLapseRequest;

/** The KYC provider reports that a business missed its re-verification. */
public interface KycBusinessLapseRecorder {

    void record(RecordBusinessLapseRequest request);
}
