package verification.usecase;

import verification.domain.Business;
import verification.domain.TeamMember;
import verification.usecase.dto.StartTeamMemberRequest;
import verification.usecase.dto.StartTeamMemberResponse;
import verification.usecase.port.in.ProviderTeamMemberStarter;
import verification.usecase.port.out.BusinessRepository;
import verification.usecase.port.out.KycProvider;
import verification.usecase.port.out.TeamMemberRepository;

public final class ProviderStartTeamMember implements ProviderTeamMemberStarter {

    private final BusinessRepository businesses;
    private final TeamMemberRepository members;
    private final KycProvider kyc;

    public ProviderStartTeamMember(BusinessRepository businesses, TeamMemberRepository members, KycProvider kyc) {
        this.businesses = businesses;
        this.members = members;
        this.kyc = kyc;
    }

    @Override
    public StartTeamMemberResponse start(StartTeamMemberRequest request) {
        Business business = Verifications.business(businesses, request.businessId());
        TeamMember member = TeamMember.start(business, Verifications.role(request.role()), request.profilePhoto());
        members.save(member);
        return new StartTeamMemberResponse(member.id(), kyc.startTeamMemberCheck(member.id(), member.profilePhoto()));
    }
}
