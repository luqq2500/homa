package account.usecase.port.in;

import account.usecase.dto.RecordMemberReverificationRequiredRequest;

/** The KYC provider's monitoring asks a team member to re-verify by a date. */
public interface KycMemberReverificationRequirer {

    void record(RecordMemberReverificationRequiredRequest request);
}
