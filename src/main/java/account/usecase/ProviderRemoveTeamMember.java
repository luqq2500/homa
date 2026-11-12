package account.usecase;

import account.domain.TeamMember;
import account.usecase.dto.ProviderRemoveTeamMemberRequest;
import account.usecase.port.in.ProviderTeamMemberRemover;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.TeamMemberRepository;
import ie.events.TeamMemberRemovedIE;
import java.time.Clock;

/**
 * A business removes one of its team members. They can no longer be named for jobs, and cannot come back.
 */
public final class ProviderRemoveTeamMember implements ProviderTeamMemberRemover {

    private final TeamMemberRepository members;
    private final AccountIEPublisher events;
    private final Clock clock;

    public ProviderRemoveTeamMember(TeamMemberRepository members, AccountIEPublisher events, Clock clock) {
        this.members = members;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void remove(ProviderRemoveTeamMemberRequest request) {
        TeamMember member = Accounts.memberOf(members, request.memberId(), request.providerId());

        member.remove();

        members.save(member);
        events.publish(new TeamMemberRemovedIE(member.id(), member.providerId(), member.role().name(), clock.instant()));
    }
}
