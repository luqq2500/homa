package verification.usecase;

import ie.events.TeamMemberCheckFailedIE;
import ie.events.TeamMemberReverifiedIE;
import java.time.Clock;
import java.time.Instant;
import verification.domain.CheckFailure;
import verification.domain.TeamMember;
import verification.domain.TeamMemberStatus;
import verification.usecase.dto.ReportTeamMemberResultRequest;
import verification.usecase.dto.ReportTeamMemberResultResponse;
import verification.usecase.exception.UnknownCheckFailureException;
import verification.usecase.port.in.KycTeamMemberResultReporter;
import verification.usecase.port.out.TeamMemberRepository;
import verification.usecase.port.out.VerificationIEPublisher;

public final class KycReportTeamMemberResult implements KycTeamMemberResultReporter {

    private static final String PASSED = "PASSED";

    private final TeamMemberRepository members;
    private final VerificationIEPublisher publisher;
    private final Clock clock;

    public KycReportTeamMemberResult(TeamMemberRepository members, VerificationIEPublisher publisher, Clock clock) {
        this.members = members;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ReportTeamMemberResultResponse report(ReportTeamMemberResultRequest request) {
        Instant now = clock.instant();
        TeamMember member = Verifications.member(members, request.memberId());
        boolean firstCheck = member.status() == TeamMemberStatus.WAITING_FOR_VERIFICATION;
        if (!PASSED.equals(request.result())) {
            failed(member, firstCheck, failure(request.result()), now);
        } else if (firstCheck) {
            member.verified(request.registeredName(), request.documentCountry(), request.verifiedUntil(), now);
            members.save(member);
        } else {
            reverified(member, request.verifiedUntil(), now);
        }
        return new ReportTeamMemberResultResponse(member.status().name());
    }

    /** A failed first check adds nothing and the provider is told why; a failed re-verification changes nothing. */
    private void failed(TeamMember member, boolean firstCheck, CheckFailure failure, Instant now) {
        if (!firstCheck) {
            return;
        }
        member.checkFailed(failure);
        members.save(member);
        publisher.publish(new TeamMemberCheckFailedIE(member.id(), member.businessId(), failure.name(), now));
    }

    private void reverified(TeamMember member, Instant verifiedUntil, Instant now) {
        boolean restored = member.reverified(verifiedUntil, now);
        members.save(member);
        if (restored) {
            publisher.publish(Verifications.restored(member, now));
        } else {
            publisher.publish(new TeamMemberReverifiedIE(member.id(), member.businessId(), member.lastVerifiedAt(), now));
        }
    }

    private static CheckFailure failure(String result) {
        try {
            return CheckFailure.valueOf(result);
        } catch (IllegalArgumentException | NullPointerException unknown) {
            throw new UnknownCheckFailureException(result);
        }
    }
}
