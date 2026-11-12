package account.domain;

import account.domain.exception.ProviderNotVerifiedException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A provider: a business registered with SSM. Homa has only business providers. The KYC provider verifies
 * the business through KYB, with its owner's or director's KYC in the same flow, re-verifies it every year,
 * and monitors it in between; Homa records the results and does no check of its own. A business starts
 * with no team members and adds its helpers and supervisors.
 */
public final class ProviderAccount {

    private final String id;
    private final PhoneNumber phone;
    private VerificationStatus status = VerificationStatus.PENDING;
    private BusinessRegistration businessRegistration;
    private Instant verifiedUntil;
    private Instant lastVerifiedAt;
    private String seriousFlag;
    private boolean payoutAccountReady;

    private ProviderAccount(String id, PhoneNumber phone) {
        this.id = id;
        this.phone = phone;
    }

    /** Registers a business with a confirmed phone number. */
    public static ProviderAccount register(PhoneVerification verified) {
        verified.requireConfirmed();
        return new ProviderAccount(new ProviderId().generate(), verified.phone());
    }

    /**
     * Records the KYC provider's KYB result. A first rejection can be redone. Once verified, a new pass
     * keeps the business verified until the new date, a lapsed business that passes is back, a repeated
     * pass changes nothing, and a failed re-verification changes nothing: the KYC provider decides when
     * the business lapses.
     */
    public VerificationOutcome recordKybResult(KybResult result, Instant now) {
        if (!result.isVerified()) {
            return recordRejection();
        }
        return recordPass(result.business().orElseThrow(), result.verifiedUntil().orElseThrow(), now);
    }

    /** The KYC provider reports that the business missed its re-verification: it takes no new requests until it passes. */
    public void lapse() {
        if (status != VerificationStatus.VERIFIED) {
            throw new ProviderNotVerifiedException(id, status);
        }
        status = VerificationStatus.LAPSED;
    }

    /**
     * The KYC provider's monitoring found a change, such as a new director, and asks for re-verification by
     * {@code deadline}. The business keeps working, and its verified-until date moves to the deadline if
     * that is sooner; it never moves later.
     */
    public void requireReverification(String reason, Instant deadline) {
        AccountText.require(reason, "A reason for re-verification");
        Objects.requireNonNull(deadline);
        if (status != VerificationStatus.VERIFIED) {
            throw new ProviderNotVerifiedException(id, status);
        }
        verifiedUntil = deadline.isBefore(verifiedUntil) ? deadline : verifiedUntil;
    }

    /**
     * The KYC provider's monitoring found a serious flag, such as the business being struck off or a
     * sanctions match: the business lapses at once. Its upcoming accepted reservations are disputed and
     * cancelled with a full refund. A later pass clears the flag.
     */
    public void recordSeriousFlag(String reason) {
        String why = AccountText.require(reason, "A reason for the flag");
        if (status != VerificationStatus.VERIFIED && status != VerificationStatus.LAPSED) {
            throw new ProviderNotVerifiedException(id, status);
        }
        status = VerificationStatus.LAPSED;
        seriousFlag = why;
    }

    /**
     * Records that the payment provider reports the business bank account as ready for payouts. Returns
     * whether it changed anything: a repeated report changes nothing.
     */
    public boolean recordPayoutAccountReady() {
        boolean changed = !payoutAccountReady;
        payoutAccountReady = true;
        return changed;
    }

    /** The provider's own part of going live: verified, not lapsed, with a ready payout account. */
    public boolean isReadyForGoLive() {
        return status == VerificationStatus.VERIFIED && payoutAccountReady;
    }

    public String id() {
        return id;
    }

    public PhoneNumber phone() {
        return phone;
    }

    public VerificationStatus verificationStatus() {
        return status;
    }

    /** The SSM registration read by KYB; empty until verified. */
    public Optional<BusinessRegistration> businessRegistration() {
        return Optional.ofNullable(businessRegistration);
    }

    /** Until when the business is verified; a job must end by this date. Empty until verified. */
    public Optional<Instant> verifiedUntil() {
        return Optional.ofNullable(verifiedUntil);
    }

    /** When the business last passed, as the Verified ID badge shows it; empty until verified. */
    public Optional<Instant> lastVerifiedAt() {
        return Optional.ofNullable(lastVerifiedAt);
    }

    public boolean isPayoutAccountReady() {
        return payoutAccountReady;
    }

    /** Why the KYC provider seriously flagged the business; empty when it is not flagged. */
    public Optional<String> seriousFlag() {
        return Optional.ofNullable(seriousFlag);
    }

    /** Not verified before: pending, or rejected and redoing it. */
    private boolean isFirstVerification() {
        return status == VerificationStatus.PENDING || status == VerificationStatus.REJECTED;
    }

    private VerificationOutcome recordRejection() {
        if (!isFirstVerification()) {
            return VerificationOutcome.RECHECK_FAILED;
        }
        status = VerificationStatus.REJECTED;
        return VerificationOutcome.REJECTED;
    }

    private VerificationOutcome recordPass(BusinessRegistration business, Instant until, Instant now) {
        boolean first = isFirstVerification();
        if (status == VerificationStatus.VERIFIED && until.equals(verifiedUntil)) {
            return VerificationOutcome.UNCHANGED;
        }
        businessRegistration = business;
        verifiedUntil = until;
        lastVerifiedAt = now;
        seriousFlag = null;
        status = VerificationStatus.VERIFIED;
        return first ? VerificationOutcome.VERIFIED : VerificationOutcome.REVERIFIED;
    }
}
