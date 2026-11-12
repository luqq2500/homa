package account.usecase.port.in;

import account.usecase.dto.RecordBusinessFlagRequest;

/** The KYC provider's monitoring reports a confirmed serious flag on a business. */
public interface KycBusinessFlagRecorder {

    void record(RecordBusinessFlagRequest request);
}
