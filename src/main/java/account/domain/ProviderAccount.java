package account.domain;

import account.domain.exception.BusinessIsNotAHelperException;
import account.domain.exception.BusinessVerifiesThroughEkybException;
import account.domain.exception.NotABusinessProviderException;
import account.domain.exception.ProviderAlreadyVerifiedException;
import account.domain.exception.ProviderNotVerifiedException;
import account.domain.exception.ReverificationNotYetDueException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A provider: an independent provider, verified through eKYC, or a business registered with SSM, verified
 * through eKYB, which checks the registration and includes its owner's or director's eKYC in one flow. The
 * person verifies with a Malaysian document, and their registered name comes from the result. Homa does no
 * check of its own. By default an independent provider is a helper on their own team and can remove
 * themselves; a business starts with no team members and adds its helpers and supervisors.
 *
 * <p>A verified provider is re-verified every re-verification period. One who misses their due date
 * lapses: they are not ready for go-live, and their listing takes no new requests until they pass.
 */
public final class ProviderAccount {

    private final String id;
    private final PhoneNumber phone;
    private final ProviderType type;
    private VerificationStatus status = VerificationStatus.PENDING;
    private IdentityDocument document;
    private Verification verification;
    private BusinessRegistration businessRegistration;
    private boolean payoutAccountReady;
    private boolean includesSelfAsHelper;

    private ProviderAccount(String id, PhoneNumber phone, ProviderType type) {
        this.id = id;
        this.phone = phone;
        this.type = type;
        this.includesSelfAsHelper = type == ProviderType.INDEPENDENT;
    }

    /** Registers with a confirmed phone number and the provider type chosen at onboarding. */
    public static ProviderAccount register(PhoneVerification verified, ProviderType type) {
        verified.requireConfirmed();
        return new ProviderAccount(new ProviderId().generate(), verified.phone(), Objects.requireNonNull(type));
    }

    /**
     * Records the eKYC result for an independent provider. Before the first verification, a rejected
     * provider can be verified on a later attempt. Once verified, the same rules as for a team member
     * apply: with a re-check open a pass starts a new period and a rejection changes nothing yet; with no
     * re-check open a repeated pass changes nothing and a rejection is refused; a lapsed provider who
     * passes is verified again. A verified document must be on the accepted list for providers: a
     * Malaysian MyKad or passport.
     */
    public EkycOutcome recordEkycResult(EkycResult result, AcceptedDocuments accepted, VerificationLimits limits, Instant now) {
        if (type == ProviderType.BUSINESS) {
            throw new BusinessVerifiesThroughEkybException(id);
        }
        if (result.isVerified()) {
            requireProviderDocument(result.verifiedDocument(), accepted);
        }
        return record(result.isVerified(), () -> document = result.verifiedDocument(), limits, now);
    }

    /**
     * Records the eKYB result for a business: its SSM registration and its owner's or director's eKYC,
     * checked together by the eKYB provider. The rules are the same as for eKYC, including re-verification
     * through eKYB. The owner's or director's document must be on the accepted list for providers.
     */
    public EkycOutcome recordEkybResult(EkybResult result, AcceptedDocuments accepted, VerificationLimits limits, Instant now) {
        if (type != ProviderType.BUSINESS) {
            throw new NotABusinessProviderException(id);
        }
        if (result.isVerified()) {
            requireProviderDocument(result.director().orElseThrow(), accepted);
        }
        return record(result.isVerified(), () -> {
            document = result.director().orElseThrow();
            businessRegistration = result.business().orElseThrow();
        }, limits, now);
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
        status = VerificationStatus.LAPSED;
    }

    /** Records that Stripe reports the provider's payout account as ready. */
    public void recordPayoutAccountReady() {
        payoutAccountReady = true;
    }

    /** An independent provider chooses whether they are a helper on their own team. A business never is. */
    public void includeSelfAsHelper(boolean include) {
        if (include && type == ProviderType.BUSINESS) {
            throw new BusinessIsNotAHelperException(id);
        }
        includesSelfAsHelper = include;
    }

    /**
     * The provider's own part of going live: verified through eKYC, or eKYB for a business, not lapsed, and
     * the payout account is ready.
     */
    public boolean isReadyForGoLive() {
        return status == VerificationStatus.VERIFIED && payoutAccountReady;
    }

    public String id() {
        return id;
    }

    public PhoneNumber phone() {
        return phone;
    }

    public ProviderType type() {
        return type;
    }

    /** The name on the provider's, or the business owner's or director's, identity document; empty until verified. */
    public Optional<String> registeredName() {
        return document().map(IdentityDocument::registeredName);
    }

    /** The document the provider last verified with; empty until verified. */
    public Optional<IdentityDocument> document() {
        return Optional.ofNullable(document);
    }

    /** A business's SSM registration from the eKYB result; empty until verified, and always for an independent provider. */
    public Optional<BusinessRegistration> businessRegistration() {
        return Optional.ofNullable(businessRegistration);
    }

    /** Where the provider's eKYC, or a business's eKYB, stands. */
    public VerificationStatus verificationStatus() {
        return status;
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

    private void requireProviderDocument(IdentityDocument read, AcceptedDocuments accepted) {
        accepted.require(DocumentHolder.PROVIDER, read.type(), read.issuingCountry());
    }

    /** The rules shared by eKYC and eKYB results; {@code onPass} keeps what the result read. */
    private EkycOutcome record(boolean verified, Runnable onPass, VerificationLimits limits, Instant now) {
        return switch (status) {
            case VERIFIED -> recordVerifiedResult(verified, onPass, limits, now);
            case LAPSED -> recordRecheckResult(verified, onPass, limits, now);
            default -> recordFirstResult(verified, onPass, limits, now);
        };
    }

    private EkycOutcome recordFirstResult(boolean verified, Runnable onPass, VerificationLimits limits, Instant now) {
        if (!verified) {
            status = VerificationStatus.REJECTED;
            return EkycOutcome.REJECTED;
        }
        pass(onPass, limits, now);
        return EkycOutcome.VERIFIED;
    }

    private EkycOutcome recordVerifiedResult(boolean verified, Runnable onPass, VerificationLimits limits, Instant now) {
        if (!verification.recheckOpen()) {
            if (!verified) {
                throw new ProviderAlreadyVerifiedException(id);
            }
            return EkycOutcome.UNCHANGED;
        }
        return recordRecheckResult(verified, onPass, limits, now);
    }

    private EkycOutcome recordRecheckResult(boolean verified, Runnable onPass, VerificationLimits limits, Instant now) {
        if (!verified) {
            return EkycOutcome.RECHECK_FAILED;
        }
        pass(onPass, limits, now);
        return EkycOutcome.REVERIFIED;
    }

    private void pass(Runnable onPass, VerificationLimits limits, Instant now) {
        onPass.run();
        status = VerificationStatus.VERIFIED;
        verification = Verification.passedAt(now, limits);
    }

    private Verification verifiedVerification() {
        if (status != VerificationStatus.VERIFIED) {
            throw new ProviderNotVerifiedException(id, status);
        }
        return verification;
    }
}
