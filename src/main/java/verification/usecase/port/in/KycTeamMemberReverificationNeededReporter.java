package verification.usecase.port.in;

import verification.usecase.dto.ReportTeamMemberReverificationNeededRequest;
import verification.usecase.dto.ReportTeamMemberReverificationNeededResponse;

/** The KYC provider reports that a team member must be re-verified by a date. */
public interface KycTeamMemberReverificationNeededReporter {

    ReportTeamMemberReverificationNeededResponse report(ReportTeamMemberReverificationNeededRequest request);
}
