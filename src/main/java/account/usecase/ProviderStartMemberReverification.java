package account.usecase;

import account.domain.TeamMember;
import account.usecase.dto.StartMemberReverificationRequest;
import account.usecase.dto.StartMemberReverificationResponse;
import account.usecase.port.in.ProviderMemberReverificationStarter;
import account.usecase.port.out.IdentityVerificationService;
import account.usecase.port.out.TeamMemberRepository;

/**
 * Opens a team member's re-verification on the provider's phone, with the person present. Nothing changes
 * in Homa until the KYC provider reports the result.
 */
public final class ProviderStartMemberReverification implements ProviderMemberReverificationStarter {

    private final TeamMemberRepository members;
    private final IdentityVerificationService kyc;

    public ProviderStartMemberReverification(TeamMemberRepository members, IdentityVerificationService kyc) {
        this.members = members;
        this.kyc = kyc;
    }

    @Override
    public StartMemberReverificationResponse start(StartMemberReverificationRequest request) {
        TeamMember member = Accounts.memberOf(members, request.memberId(), request.providerId());
        return new StartMemberReverificationResponse(kyc.startMemberReverification(member.id()));
    }
}
