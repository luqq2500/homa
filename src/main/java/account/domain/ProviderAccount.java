package account.domain;

import account.domain.exception.ProviderAlreadyVerifiedException;
import account.domain.exception.ProviderNotVerifiedException;
import account.domain.exception.ReverificationNotYetDueException;
import java.time.Instant;
import java.util.Optional;

/**
 * An individual provider. A provider verifies through eKYC and adds a payout account before going live.
 * By default the provider is their own helper; a provider who runs a team can choose not to be.
 *
 * <p>A verified provider is re-verified every re-verification period. One who misses their due date
 * lapses: they are not ready for go-live, and their listing takes no new requests until they pass.
 */
public final class ProviderAccount {

    private final String id;
    private final PhoneNumber phone;
    private final String legalName;
    private VerificationStatus ekyc = VerificationStatus.PENDING;
    private Verification verification;
    private boolean payoutAccountReady;
    private boolean includesSelfAsHelper = true;

    private ProviderAccount(String id, PhoneNumber phone, String legalName) {
        this.id = id;
        this.phone = phone;
        this.legalName = legalName;
    }

    public static ProviderAccount register(PhoneVerification verified, String legalName) {
        verified.requireConfirmed();
        return new ProviderAccount(
                new ProviderId().generate(), verified.phone(), AccountText.require(legalName, "A provider's legal name"));
    }

    /**
     * Records Kenal's result. Before the first verification, a rejected provider can be verified on a later
     * attempt. Once verified, the same rules as for a team member apply: with a re-check open a pass starts
     * a new period and a rejection changes nothing yet; with no re-check open a repeated pass changes
     * nothing and a rejection is refused; a lapsed provider who passes is verified again.
     */
    public EkycOutcome recordEkycResult(VerificationStatus result, VerificationLimits limits, Instant now) {
        boolean verified = EkycResult.isVerified(result);
        return switch (ekyc) {
            case VERIFIED -> recordVerifiedResult(verified, limits, now);
            case LAPSED -> recordRecheckResult(verified, limits, now);
            default -> recordFirstResult(verified, limits, now);
        };
    }

    /** Opens the regular re-check, from the re-verification reminder before the due date. */
    public void openRecheck(VerificationLimits limits, Instant now) {
        verification = verifiedVerification().withRecheckOpened(id, limits, now);
    }

    /** Homa asks for an early re-check, with a reason; the due date moves sooner, never later. */
    public void requestEarlyRecheck(String reason, VerificationLimits limits, Instant now) {
        verification = verifiedVerification().withEarlyRecheck(reason, limits, now);
    }

    /** A verified provider whose due date has come without passing re-verification lapses. */
    public void lapse(Instant now) {
        Verification current = verifiedVerification();
        if (!current.isDue(now)) {
            throw new ReverificationNotYetDueException(current.dueAt());
        }
        ekyc = VerificationStatus.LAPSED;
    }

    /** Records that Stripe reports the provider's payout account as ready. */
    public void recordPayoutAccountReady() {
        payoutAccountReady = true;
    }

    public void includeSelfAsHelper(boolean include) {
        includesSelfAsHelper = include;
    }

    /**
     * The provider's own part of going live: their eKYC is verified, not lapsed, and their payout account
     * is ready.
     */
    public boolean isReadyForGoLive() {
        return ekyc == VerificationStatus.VERIFIED && payoutAccountReady;
    }

    public String id() {
        return id;
    }

    public PhoneNumber phone() {
        return phone;
    }

    public String legalName() {
        return legalName;
    }

    public VerificationStatus ekycStatus() {
        return ekyc;
    }

    /** When the provider was last verified and when re-verification is due; empty until first verified. */
    public Optional<Verification> verification() {
        return Optional.ofNullable(verification);
    }

    public boolean isPayoutAccountReady() {
        return payoutAccountReady;
    }

    public boolean includesSelfAsHelper() {
        return includesSelfAsHelper;
    }

    private EkycOutcome recordFirstResult(boolean verified, VerificationLimits limits, Instant now) {
        if (!verified) {
            ekyc = VerificationStatus.REJECTED;
            return EkycOutcome.REJECTED;
        }
        pass(limits, now);
        return EkycOutcome.VERIFIED;
    }

    private EkycOutcome recordVerifiedResult(boolean verified, VerificationLimits limits, Instant now) {
        if (!verification.recheckOpen()) {
            if (!verified) {
                throw new ProviderAlreadyVerifiedException(id);
            }
            return EkycOutcome.UNCHANGED;
        }
        return recordRecheckResult(verified, limits, now);
    }

    private EkycOutcome recordRecheckResult(boolean verified, VerificationLimits limits, Instant now) {
        if (!verified) {
            return EkycOutcome.RECHECK_FAILED;
        }
        pass(limits, now);
        return EkycOutcome.REVERIFIED;
    }

    private void pass(VerificationLimits limits, Instant now) {
        ekyc = VerificationStatus.VERIFIED;
        verification = Verification.passedAt(now, limits);
    }

    private Verification verifiedVerification() {
        if (ekyc != VerificationStatus.VERIFIED) {
            throw new ProviderNotVerifiedException(id, ekyc);
        }
        return verification;
    }
}
