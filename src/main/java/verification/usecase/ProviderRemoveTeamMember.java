package verification.usecase;

import java.time.Clock;
import java.time.Instant;
import verification.domain.TeamMember;
import verification.usecase.dto.RemoveTeamMemberRequest;
import verification.usecase.dto.RemoveTeamMemberResponse;
import verification.usecase.port.in.ProviderTeamMemberRemover;
import verification.usecase.port.out.TeamMemberRepository;
import verification.usecase.port.out.VerificationIEPublisher;

public final class ProviderRemoveTeamMember implements ProviderTeamMemberRemover {

    private final TeamMemberRepository members;
    private final VerificationIEPublisher publisher;
    private final Clock clock;

    public ProviderRemoveTeamMember(TeamMemberRepository members, VerificationIEPublisher publisher, Clock clock) {
        this.members = members;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public RemoveTeamMemberResponse remove(RemoveTeamMemberRequest request) {
        Instant now = clock.instant();
        TeamMember member = Verifications.member(members, request.businessId(), request.memberId());
        boolean wasOnTeam = member.removeByProvider(now);
        members.save(member);
        if (wasOnTeam) {
            publisher.publish(Verifications.left(member, Verifications.REMOVED, now));
        }
        return new RemoveTeamMemberResponse(member.id());
    }
}
