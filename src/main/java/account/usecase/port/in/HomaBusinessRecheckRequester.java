package account.usecase.port.in;

import account.usecase.dto.HomaRequestBusinessRecheckRequest;

/** Homa's founders ask the KYC provider for an early re-check of a business. */
public interface HomaBusinessRecheckRequester {

    void request(HomaRequestBusinessRecheckRequest request);
}
