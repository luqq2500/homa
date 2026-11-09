package account.domain;

import account.domain.exception.BusinessRegistrationAlreadyCheckedException;
import account.domain.exception.NotABusinessProviderException;
import account.domain.exception.ProviderAlreadyVerifiedException;
import account.domain.exception.ProviderNotVerifiedException;
import account.domain.exception.ReverificationNotYetDueException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A provider: an independent provider, or a business registered with SSM, verified through its owner or
 * director. The provider verifies through eKYC with a Malaysian document, and their registered name comes
 * from the eKYC result. A business's registration is also checked by Homa. By default the provider is a
 * helper on their own team, and can remove themselves.
 *
 * <p>A verified provider is re-verified every re-verification period. One who misses their due date
 * lapses: they are not ready for go-live, and their listing takes no new requests until they pass.
 */
public final class ProviderAccount {

    private final String id;
    private final PhoneNumber phone;
    private final ProviderType type;
    private VerificationStatus ekyc = VerificationStatus.PENDING;
    private IdentityDocument document;
    private Verification verification;
    private BusinessRegistration businessRegistration;
    private boolean payoutAccountReady;
    private boolean includesSelfAsHelper = true;

    private ProviderAccount(String id, PhoneNumber phone, ProviderType type) {
        this.id = id;
        this.phone = phone;
        this.type = type;
    }

    /** Registers with a confirmed phone number and the provider type chosen at onboarding. */
    public static ProviderAccount register(PhoneVerification verified, ProviderType type) {
        verified.requireConfirmed();
        return new ProviderAccount(new ProviderId().generate(), verified.phone(), Objects.requireNonNull(type));
    }

    /** Records that Homa checked a business provider's SSM registration, once. */
    public void recordBusinessRegistrationChecked(String ssmNumber, Instant now) {
        if (type != ProviderType.BUSINESS) {
            throw new NotABusinessProviderException(id);
        }
        if (businessRegistration != null) {
            throw new BusinessRegistrationAlreadyCheckedException(id);
        }
        businessRegistration = new BusinessRegistration(ssmNumber, now);
    }

    /**
     * Records Kenal's result. Before the first verification, a rejected provider can be verified on a later
     * attempt. Once verified, the same rules as for a team member apply: with a re-check open a pass starts
     * a new period and a rejection changes nothing yet; with no re-check open a repeated pass changes
     * nothing and a rejection is refused; a lapsed provider who passes is verified again. A verified
     * document must be on the accepted list for providers: a Malaysian MyKad or passport.
     */
    public EkycOutcome recordEkycResult(EkycResult result, AcceptedDocuments accepted, VerificationLimits limits, Instant now) {
        if (result.isVerified()) {
            IdentityDocument read = result.verifiedDocument();
            accepted.require(DocumentHolder.PROVIDER, read.type(), read.issuingCountry());
        }
        return switch (ekyc) {
            case VERIFIED -> recordVerifiedResult(result, limits, now);
            case LAPSED -> recordRecheckResult(result, limits, now);
            default -> recordFirstResult(result, limits, now);
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
     * The provider's own part of going live: their eKYC is verified, not lapsed, their payout account is
     * ready, and for a business, its registration is checked.
     */
    public boolean isReadyForGoLive() {
        return ekyc == VerificationStatus.VERIFIED && payoutAccountReady && isRegistrationReady();
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

    /** The name on the provider's identity document, from the eKYC result; empty until verified. */
    public Optional<String> registeredName() {
        return document().map(IdentityDocument::registeredName);
    }

    /** The document the provider last verified with; empty until verified. */
    public Optional<IdentityDocument> document() {
        return Optional.ofNullable(document);
    }

    /** A business's checked SSM registration; empty until Homa checks it, and always for an independent provider. */
    public Optional<BusinessRegistration> businessRegistration() {
        return Optional.ofNullable(businessRegistration);
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

    /** An independent provider has no registration to check; a business needs Homa's check. */
    private boolean isRegistrationReady() {
        return type == ProviderType.INDEPENDENT || businessRegistration != null;
    }

    private EkycOutcome recordFirstResult(EkycResult result, VerificationLimits limits, Instant now) {
        if (!result.isVerified()) {
            ekyc = VerificationStatus.REJECTED;
            return EkycOutcome.REJECTED;
        }
        pass(result, limits, now);
        return EkycOutcome.VERIFIED;
    }

    private EkycOutcome recordVerifiedResult(EkycResult result, VerificationLimits limits, Instant now) {
        if (!verification.recheckOpen()) {
            if (!result.isVerified()) {
                throw new ProviderAlreadyVerifiedException(id);
            }
            return EkycOutcome.UNCHANGED;
        }
        return recordRecheckResult(result, limits, now);
    }

    private EkycOutcome recordRecheckResult(EkycResult result, VerificationLimits limits, Instant now) {
        if (!result.isVerified()) {
            return EkycOutcome.RECHECK_FAILED;
        }
        pass(result, limits, now);
        return EkycOutcome.REVERIFIED;
    }

    private void pass(EkycResult result, VerificationLimits limits, Instant now) {
        ekyc = VerificationStatus.VERIFIED;
        document = result.verifiedDocument();
        verification = Verification.passedAt(now, limits);
    }

    private Verification verifiedVerification() {
        if (ekyc != VerificationStatus.VERIFIED) {
            throw new ProviderNotVerifiedException(id, ekyc);
        }
        return verification;
    }
}
