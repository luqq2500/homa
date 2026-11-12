package account.usecase;

import account.domain.TeamMember;
import account.usecase.dto.HomaRequestMemberRecheckRequest;
import account.usecase.port.in.HomaMemberRecheckRequester;
import account.usecase.port.out.AdminAuditLog;
import account.usecase.port.out.IdentityVerificationService;
import account.usecase.port.out.TeamMemberRepository;
import java.time.Clock;

/**
 * Asks the KYC provider for an early re-verification of a team member, with a reason, and records it.
 * Nothing changes in Homa until the KYC provider reports a pass or a lapse.
 */
public final class HomaRequestMemberRecheck implements HomaMemberRecheckRequester {

    private final TeamMemberRepository members;
    private final IdentityVerificationService kyc;
    private final AdminActions admin;
    private final Clock clock;

    public HomaRequestMemberRecheck(TeamMemberRepository members, IdentityVerificationService kyc, AdminAuditLog auditLog, Clock clock) {
        this.members = members;
        this.kyc = kyc;
        this.admin = new AdminActions(auditLog);
        this.clock = clock;
    }

    @Override
    public void request(HomaRequestMemberRecheckRequest request) {
        String founder = AdminActions.founder(request.decidedBy());
        String reason = AdminActions.reason(request.reason(), "ask for an early re-check");
        TeamMember member = Accounts.member(members, request.memberId());

        kyc.requestMemberRecheck(member.id(), reason);

        admin.record(founder, "REQUEST_MEMBER_RECHECK", member.id(), reason, clock.instant());
    }
}
