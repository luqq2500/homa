package verification.usecase;

import ie.events.TeamMemberRoleChangedIE;
import java.time.Clock;
import verification.domain.TeamMember;
import verification.usecase.dto.ChangeTeamMemberRoleRequest;
import verification.usecase.dto.ChangeTeamMemberRoleResponse;
import verification.usecase.port.in.ProviderTeamMemberRoleChanger;
import verification.usecase.port.out.TeamMemberRepository;
import verification.usecase.port.out.VerificationIEPublisher;

public final class ProviderChangeTeamMemberRole implements ProviderTeamMemberRoleChanger {

    private final TeamMemberRepository members;
    private final VerificationIEPublisher publisher;
    private final Clock clock;

    public ProviderChangeTeamMemberRole(TeamMemberRepository members, VerificationIEPublisher publisher, Clock clock) {
        this.members = members;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ChangeTeamMemberRoleResponse change(ChangeTeamMemberRoleRequest request) {
        TeamMember member = Verifications.member(members, request.businessId(), request.memberId());
        if (member.changeRole(Verifications.role(request.role()))) {
            members.save(member);
            publisher.publish(new TeamMemberRoleChangedIE(member.id(), member.businessId(), member.role().name(),
                    clock.instant()));
        }
        return new ChangeTeamMemberRoleResponse(member.id(), member.role().name());
    }
}
