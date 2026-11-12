package account.usecase;

import account.domain.KycResult;
import account.domain.TeamMember;
import account.domain.VerificationOutcome;
import account.usecase.dto.RecordTeamMemberResultRequest;
import account.usecase.port.in.KycTeamMemberResultRecorder;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.TeamMemberRepository;
import ie.events.RecheckFailedIE;
import ie.events.TeamMemberReverifiedIE;
import java.time.Clock;
import java.time.Instant;

/**
 * Records the KYC provider's re-verification result for a team member, and publishes one event for the
 * outcome (R1). A pass keeps them verified until the new date, or brings a lapsed member back; a failed
 * re-check changes nothing but is told, so they can retry. A repeated pass is not saved or told.
 */
public final class KycRecordTeamMemberResult implements KycTeamMemberResultRecorder {

    private final TeamMemberRepository members;
    private final AccountIEPublisher events;
    private final Clock clock;

    public KycRecordTeamMemberResult(TeamMemberRepository members, AccountIEPublisher events, Clock clock) {
        this.members = members;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void record(RecordTeamMemberResultRequest request) {
        TeamMember member = Accounts.member(members, request.memberId());
        Instant now = clock.instant();
        KycResult result = request.verified() ? KycResult.verified(request.registeredName(), request.verifiedUntil()) : KycResult.rejected();

        VerificationOutcome outcome = member.recordKycResult(result, now);

        if (outcome == VerificationOutcome.REVERIFIED) {
            members.save(member);
            events.publish(new TeamMemberReverifiedIE(member.id(), member.providerId(), member.role().name(), member.verifiedUntil(), now));
        } else if (outcome == VerificationOutcome.RECHECK_FAILED) {
            events.publish(new RecheckFailedIE(member.providerId(), member.id(), now));
        }
    }
}
