package verification.usecase;

import verification.domain.TeamMember;
import verification.usecase.dto.StartTeamMemberReverificationRequest;
import verification.usecase.dto.StartTeamMemberReverificationResponse;
import verification.usecase.port.in.ProviderTeamMemberReverificationStarter;
import verification.usecase.port.out.KycProvider;
import verification.usecase.port.out.TeamMemberRepository;

public final class ProviderStartTeamMemberReverification implements ProviderTeamMemberReverificationStarter {

    private final TeamMemberRepository members;
    private final KycProvider kyc;

    public ProviderStartTeamMemberReverification(TeamMemberRepository members, KycProvider kyc) {
        this.members = members;
        this.kyc = kyc;
    }

    @Override
    public StartTeamMemberReverificationResponse start(StartTeamMemberReverificationRequest request) {
        TeamMember member = Verifications.member(members, request.businessId(), request.memberId());
        return new StartTeamMemberReverificationResponse(kyc.startTeamMemberCheck(member.id(), member.profilePhoto()));
    }
}
