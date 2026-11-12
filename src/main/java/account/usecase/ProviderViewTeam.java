package account.usecase;

import account.domain.MemberVerification;
import account.domain.TeamMember;
import account.domain.TeamStatus;
import account.usecase.dto.ProviderViewTeamRequest;
import account.usecase.dto.ProviderViewTeamResponse;
import account.usecase.port.in.ProviderTeamViewer;
import account.usecase.port.out.MemberVerificationRepository;
import account.usecase.port.out.TeamMemberRepository;
import java.util.List;

/**
 * Shows a business its team, with when each member is next due for re-verification, and the people still
 * waiting for verification. Removed members are not shown.
 */
public final class ProviderViewTeam implements ProviderTeamViewer {

    private final TeamMemberRepository members;
    private final MemberVerificationRepository verifications;

    public ProviderViewTeam(TeamMemberRepository members, MemberVerificationRepository verifications) {
        this.members = members;
        this.verifications = verifications;
    }

    @Override
    public ProviderViewTeamResponse view(ProviderViewTeamRequest request) {
        List<ProviderViewTeamResponse.Member> team = members.findFor(request.providerId()).stream()
                .filter(member -> member.teamStatus() != TeamStatus.REMOVED)
                .map(ProviderViewTeam::toView)
                .toList();
        List<ProviderViewTeamResponse.Waiting> waiting = verifications.findWaitingFor(request.providerId()).stream()
                .map(ProviderViewTeam::toView)
                .toList();
        return new ProviderViewTeamResponse(team, waiting);
    }

    private static ProviderViewTeamResponse.Member toView(TeamMember member) {
        return new ProviderViewTeamResponse.Member(
                member.id(), member.registeredName(), member.role().name(), member.teamStatus().name(),
                member.verifiedUntil(), member.lastVerifiedAt());
    }

    private static ProviderViewTeamResponse.Waiting toView(MemberVerification verification) {
        return new ProviderViewTeamResponse.Waiting(verification.id(), verification.role().name(), verification.startedAt());
    }
}
