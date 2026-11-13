package verification.usecase.port.in;

import verification.usecase.dto.ReportTeamMemberResultRequest;
import verification.usecase.dto.ReportTeamMemberResultResponse;

/** The KYC provider reports the final result of a person's check: PASSED, REJECTED, PHOTO_MISMATCH, UNDER_18 or TIMED_OUT. */
public interface KycTeamMemberResultReporter {

    ReportTeamMemberResultResponse report(ReportTeamMemberResultRequest request);
}
