package verification.usecase;

import java.time.Clock;
import java.time.Instant;
import verification.domain.TeamMember;
import verification.usecase.dto.ConfirmTeamMemberRequest;
import verification.usecase.dto.ConfirmTeamMemberResponse;
import verification.usecase.port.in.ProviderTeamMemberConfirmer;
import verification.usecase.port.out.TeamMemberRepository;
import verification.usecase.port.out.VerificationIEPublisher;

public final class ProviderConfirmTeamMember implements ProviderTeamMemberConfirmer {

    private final TeamMemberRepository members;
    private final VerificationIEPublisher publisher;
    private final Clock clock;

    public ProviderConfirmTeamMember(TeamMemberRepository members, VerificationIEPublisher publisher, Clock clock) {
        this.members = members;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ConfirmTeamMemberResponse confirm(ConfirmTeamMemberRequest request) {
        Instant now = clock.instant();
        TeamMember member = Verifications.member(members, request.businessId(), request.memberId());
        member.confirm(request.lawfullyEmployed(), request.holdsValidWorkPass(), request.termsVersion(), now);
        members.save(member);
        publisher.publish(Verifications.joined(member, now));
        return new ConfirmTeamMemberResponse(member.id(), member.registeredName(), member.role().name());
    }
}
