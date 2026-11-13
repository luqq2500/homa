package verification.usecase;

import java.time.Clock;
import verification.domain.TeamMember;
import verification.usecase.dto.ReportTeamMemberLapseRequest;
import verification.usecase.dto.ReportTeamMemberLapseResponse;
import verification.usecase.port.in.KycTeamMemberLapseReporter;
import verification.usecase.port.out.TeamMemberRepository;
import verification.usecase.port.out.VerificationIEPublisher;

public final class KycReportTeamMemberLapse implements KycTeamMemberLapseReporter {

    static final String SERIOUS_FLAG = "SERIOUS_FLAG";

    private final TeamMemberRepository members;
    private final VerificationIEPublisher publisher;
    private final Clock clock;

    public KycReportTeamMemberLapse(TeamMemberRepository members, VerificationIEPublisher publisher, Clock clock) {
        this.members = members;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ReportTeamMemberLapseResponse report(ReportTeamMemberLapseRequest request) {
        TeamMember member = Verifications.member(members, request.memberId());
        if (member.lapse()) {
            members.save(member);
            String reason = request.seriousFlag() ? SERIOUS_FLAG : Verifications.LAPSED;
            publisher.publish(Verifications.left(member, reason, clock.instant()));
        }
        return new ReportTeamMemberLapseResponse(member.status().name());
    }
}
