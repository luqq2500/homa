package verification.usecase.port.in;

import verification.usecase.dto.ReportTeamMemberLapseRequest;
import verification.usecase.dto.ReportTeamMemberLapseResponse;

/** The KYC provider reports that a team member missed their re-verification, or confirmed a serious flag on them. */
public interface KycTeamMemberLapseReporter {

    ReportTeamMemberLapseResponse report(ReportTeamMemberLapseRequest request);
}
