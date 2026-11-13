package verification.usecase;

import java.time.Clock;
import java.time.Instant;
import verification.domain.TeamMember;
import verification.usecase.dto.RemoveTeamMemberByHomaRequest;
import verification.usecase.dto.RemoveTeamMemberByHomaResponse;
import verification.usecase.port.in.AdminTeamMemberRemover;
import verification.usecase.port.out.TeamMemberRepository;
import verification.usecase.port.out.VerificationIEPublisher;

public final class AdminRemoveTeamMember implements AdminTeamMemberRemover {

    private final TeamMemberRepository members;
    private final VerificationIEPublisher publisher;
    private final Clock clock;

    public AdminRemoveTeamMember(TeamMemberRepository members, VerificationIEPublisher publisher, Clock clock) {
        this.members = members;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public RemoveTeamMemberByHomaResponse remove(RemoveTeamMemberByHomaRequest request) {
        Instant now = clock.instant();
        TeamMember member = Verifications.member(members, request.memberId());
        boolean wasOnTeam = member.removeByHoma(request.adminId(), request.reason(), now);
        members.save(member);
        if (wasOnTeam) {
            publisher.publish(Verifications.left(member, Verifications.REMOVED, now));
        }
        return new RemoveTeamMemberByHomaResponse(member.id());
    }
}
