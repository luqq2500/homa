package account.usecase;

import account.domain.KycResult;
import account.domain.MemberVerification;
import account.domain.PhotoMatch;
import account.domain.VerificationOutcome;
import account.usecase.dto.RecordMemberVerificationResultRequest;
import account.usecase.port.in.KycMemberVerificationResultRecorder;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.MemberVerificationRepository;
import ie.events.MemberVerificationRejectedIE;
import ie.events.MemberVerifiedIE;
import java.time.Clock;
import java.time.Instant;

/**
 * Records the KYC provider's result for a person being added, with its comparison of the profile photo and
 * the person's date of birth. A pass waits for the provider's confirmation (R7); a rejection, an age under
 * 18 or a photo that doesn't match ends the verification, adds nobody, and says why. A repeated result
 * changes nothing, so nothing is saved or told.
 */
public final class KycRecordMemberVerificationResult implements KycMemberVerificationResultRecorder {

    private final MemberVerificationRepository verifications;
    private final AccountIEPublisher events;
    private final Clock clock;

    public KycRecordMemberVerificationResult(MemberVerificationRepository verifications, AccountIEPublisher events, Clock clock) {
        this.verifications = verifications;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void record(RecordMemberVerificationResultRequest request) {
        MemberVerification verification = Accounts.memberVerification(verifications, request.verificationId());
        Instant now = clock.instant();

        PhotoMatch photo = request.photoMatches() ? PhotoMatch.MATCHED : PhotoMatch.NOT_MATCHED;

        VerificationOutcome outcome = verification.recordKycResult(result(request), photo, now);

        if (outcome == VerificationOutcome.VERIFIED) {
            verifications.save(verification);
            events.publish(new MemberVerifiedIE(
                    verification.id(), verification.providerId(), verification.registeredName().orElseThrow(), now));
        } else if (outcome == VerificationOutcome.REJECTED) {
            verifications.save(verification);
            events.publish(new MemberVerificationRejectedIE(
                    verification.id(), verification.providerId(), verification.rejection().orElseThrow().name(), now));
        }
    }

    private static KycResult result(RecordMemberVerificationResultRequest request) {
        return request.verified()
                ? KycResult.verified(request.registeredName(), request.verifiedUntil(), request.dateOfBirth())
                : KycResult.rejected();
    }
}
