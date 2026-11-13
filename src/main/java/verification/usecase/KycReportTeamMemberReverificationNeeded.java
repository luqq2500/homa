package verification.usecase;

import verification.domain.TeamMember;
import verification.usecase.dto.ReportTeamMemberReverificationNeededRequest;
import verification.usecase.dto.ReportTeamMemberReverificationNeededResponse;
import verification.usecase.port.in.KycTeamMemberReverificationNeededReporter;
import verification.usecase.port.out.TeamMemberRepository;

public final class KycReportTeamMemberReverificationNeeded implements KycTeamMemberReverificationNeededReporter {

    private final TeamMemberRepository members;

    public KycReportTeamMemberReverificationNeeded(TeamMemberRepository members) {
        this.members = members;
    }

    @Override
    public ReportTeamMemberReverificationNeededResponse report(ReportTeamMemberReverificationNeededRequest request) {
        TeamMember member = Verifications.member(members, request.memberId());
        if (member.reverificationNeededBy(request.neededBy())) {
            members.save(member);
        }
        return new ReportTeamMemberReverificationNeededResponse(member.verifiedUntil());
    }
}
