package account.usecase;

import account.domain.KycResult;
import account.domain.MemberVerification;
import account.domain.TeamMember;
import account.domain.VerificationOutcome;
import account.usecase.dto.RecordMemberVerificationResultRequest;
import account.usecase.port.in.KycMemberVerificationResultRecorder;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.MemberVerificationRepository;
import account.usecase.port.out.TeamMemberRepository;
import ie.events.MemberVerificationRejectedIE;
import ie.events.TeamMemberAddedIE;
import java.time.Clock;
import java.time.Instant;

/**
 * Records the KYC provider's result for a person being added. A pass adds them to the team straight away,
 * with the registered name from the result (R7); a rejection ends the verification and adds nobody. A
 * repeated result changes nothing, so nothing is saved or told.
 */
public final class KycRecordMemberVerificationResult implements KycMemberVerificationResultRecorder {

    private final MemberVerificationRepository verifications;
    private final TeamMemberRepository members;
    private final AccountIEPublisher events;
    private final Clock clock;

    public KycRecordMemberVerificationResult(
            MemberVerificationRepository verifications, TeamMemberRepository members, AccountIEPublisher events, Clock clock) {
        this.verifications = verifications;
        this.members = members;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void record(RecordMemberVerificationResultRequest request) {
        MemberVerification verification = Accounts.memberVerification(verifications, request.verificationId());
        Instant now = clock.instant();

        VerificationOutcome outcome = verification.recordKycResult(result(request), now);

        if (outcome == VerificationOutcome.VERIFIED) {
            add(verification, now);
        } else if (outcome == VerificationOutcome.REJECTED) {
            verifications.save(verification);
            events.publish(new MemberVerificationRejectedIE(verification.id(), verification.providerId(), now));
        }
    }

    private void add(MemberVerification verification, Instant now) {
        TeamMember member = TeamMember.add(verification, now);
        verifications.save(verification);
        members.save(member);
        events.publish(new TeamMemberAddedIE(
                member.id(), member.providerId(), verification.id(), member.role().name(), member.registeredName(),
                member.employmentConfirmation().termsVersion(), member.employmentConfirmation().givenAt(), now));
    }

    private static KycResult result(RecordMemberVerificationResultRequest request) {
        return request.verified() ? KycResult.verified(request.registeredName(), request.verifiedUntil()) : KycResult.rejected();
    }
}
