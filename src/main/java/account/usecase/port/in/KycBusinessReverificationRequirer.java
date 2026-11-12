package account.usecase.port.in;

import account.usecase.dto.RecordBusinessReverificationRequiredRequest;

/** The KYC provider's monitoring asks a business to re-verify by a date. */
public interface KycBusinessReverificationRequirer {

    void record(RecordBusinessReverificationRequiredRequest request);
}
