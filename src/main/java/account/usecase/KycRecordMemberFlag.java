package account.usecase;

import account.domain.TeamMember;
import account.usecase.dto.RecordMemberFlagRequest;
import account.usecase.port.in.KycMemberFlagRecorder;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.TeamMemberRepository;
import ie.events.TeamMemberLapsedIE;
import java.time.Clock;

/**
 * The KYC provider's monitoring reports a confirmed serious flag on a team member, such as a sanctions
 * match: they lapse at once and leave the team until they pass.
 */
public final class KycRecordMemberFlag implements KycMemberFlagRecorder {

    private final TeamMemberRepository members;
    private final AccountIEPublisher events;
    private final Clock clock;

    public KycRecordMemberFlag(TeamMemberRepository members, AccountIEPublisher events, Clock clock) {
        this.members = members;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void record(RecordMemberFlagRequest request) {
        TeamMember member = Accounts.member(members, request.memberId());

        member.lapse();

        members.save(member);
        events.publish(new TeamMemberLapsedIE(member.id(), member.providerId(), member.role().name(), clock.instant()));
    }
}
