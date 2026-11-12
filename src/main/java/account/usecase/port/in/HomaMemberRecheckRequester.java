package account.usecase.port.in;

import account.usecase.dto.HomaRequestMemberRecheckRequest;

/** Homa's founders ask the KYC provider for an early re-check of a team member. */
public interface HomaMemberRecheckRequester {

    void request(HomaRequestMemberRecheckRequest request);
}
