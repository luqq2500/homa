package account.usecase;

import account.domain.TeamMember;
import account.usecase.dto.RecordMemberReverificationRequiredRequest;
import account.usecase.port.in.KycMemberReverificationRequirer;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.TeamMemberRepository;
import ie.events.ReverificationRequiredIE;
import java.time.Clock;

/**
 * The KYC provider's monitoring asks a team member to re-verify by a date, for example before their
 * document expires. They stay on the team, and their verified-until date moves to the deadline if that
 * is sooner.
 */
public final class KycRecordMemberReverificationRequired implements KycMemberReverificationRequirer {

    private final TeamMemberRepository members;
    private final AccountIEPublisher events;
    private final Clock clock;

    public KycRecordMemberReverificationRequired(TeamMemberRepository members, AccountIEPublisher events, Clock clock) {
        this.members = members;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void record(RecordMemberReverificationRequiredRequest request) {
        TeamMember member = Accounts.member(members, request.memberId());

        member.requireReverification(request.reason(), request.deadline());

        members.save(member);
        events.publish(new ReverificationRequiredIE(
                member.providerId(), member.id(), request.reason().strip(), member.verifiedUntil(), clock.instant()));
    }
}
