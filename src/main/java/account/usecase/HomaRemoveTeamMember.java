package account.usecase;

import account.domain.TeamMember;
import account.usecase.dto.HomaRemoveTeamMemberRequest;
import account.usecase.port.in.HomaTeamMemberRemover;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.AdminAuditLog;
import account.usecase.port.out.TeamMemberRepository;
import ie.events.TeamMemberRemovedIE;
import java.time.Clock;
import java.time.Instant;

/**
 * Homa's founders remove a team member who may not work, with a reason, and record what happened. They
 * can no longer be named for jobs, and cannot come back.
 */
public final class HomaRemoveTeamMember implements HomaTeamMemberRemover {

    private final TeamMemberRepository members;
    private final AccountIEPublisher events;
    private final AdminActions admin;
    private final Clock clock;

    public HomaRemoveTeamMember(TeamMemberRepository members, AccountIEPublisher events, AdminAuditLog auditLog, Clock clock) {
        this.members = members;
        this.events = events;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public void remove(HomaRemoveTeamMemberRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        String reason = AdminActions.reason(request.reason(), "remove a team member");
        TeamMember member = Accounts.member(members, request.memberId());
        Instant now = clock.instant();

        member.remove();

        members.save(member);
        events.publish(new TeamMemberRemovedIE(member.id(), member.providerId(), member.role().name(), now));
        admin.record(founder, "REMOVE_TEAM_MEMBER", member.id(), reason, now);
    }
}
