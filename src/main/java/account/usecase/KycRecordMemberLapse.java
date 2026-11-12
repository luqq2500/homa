package account.usecase;

import account.domain.TeamMember;
import account.usecase.dto.RecordMemberLapseRequest;
import account.usecase.port.in.KycMemberLapseRecorder;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.TeamMemberRepository;
import ie.events.TeamMemberLapsedIE;
import java.time.Clock;

/**
 * The KYC provider reports that a team member missed their re-verification: they leave the team until
 * they pass.
 */
public final class KycRecordMemberLapse implements KycMemberLapseRecorder {

    private final TeamMemberRepository members;
    private final AccountIEPublisher events;
    private final Clock clock;

    public KycRecordMemberLapse(TeamMemberRepository members, AccountIEPublisher events, Clock clock) {
        this.members = members;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void record(RecordMemberLapseRequest request) {
        TeamMember member = Accounts.member(members, request.memberId());

        member.lapse();

        members.save(member);
        events.publish(new TeamMemberLapsedIE(member.id(), member.providerId(), member.role().name(), clock.instant()));
    }
}
