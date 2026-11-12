package account.domain;

import account.domain.exception.EmploymentNotConfirmedException;
import account.domain.exception.PhotoRequiredException;
import account.domain.exception.ProviderNotVerifiedException;
import account.domain.exception.VerificationNotWaitingException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Adding a team member, before the person exists on the team. The provider fills in one form: the role, a
 * profile photo, and the lawful-employment confirmation for this person. The person then verifies with the
 * KYC provider, on the spot on the provider's phone; which documents are accepted, the face match and any
 * session timeout are the KYC provider's. Until it is verified this is shown to the provider as waiting for
 * verification: not on the team, never shown to customers, and it cannot be named for jobs.
 */
public final class MemberVerification {

    private final String id;
    private final String providerId;
    private final TeamRole role;
    private final Photo photo;
    private final EmploymentConfirmation confirmation;
    private final Instant startedAt;
    private MemberVerificationStatus status = MemberVerificationStatus.WAITING;
    private String registeredName;
    private Instant verifiedUntil;
    private Instant verifiedAt;

    private MemberVerification(String providerId, TeamRole role, Photo photo, EmploymentConfirmation confirmation, Instant now) {
        this.id = new MemberVerificationId().generate();
        this.providerId = providerId;
        this.role = role;
        this.photo = photo;
        this.confirmation = confirmation;
        this.startedAt = now;
    }

    /**
     * Starts adding a team member for {@code business}. Only a business whose KYB has passed, and that has
     * not lapsed, adds team members, so nobody's identity is checked for a business that may never be
     * verified. Refused without a photo or without the lawful-employment confirmation.
     */
    public static MemberVerification start(
            ProviderAccount business, TeamRole role, Photo photo, EmploymentConfirmation confirmation, Instant now) {
        if (business.verificationStatus() != VerificationStatus.VERIFIED) {
            throw new ProviderNotVerifiedException(business.id(), business.verificationStatus());
        }
        String provider = business.id();
        Objects.requireNonNull(role);
        if (photo == null) {
            throw new PhotoRequiredException();
        }
        if (confirmation == null) {
            throw new EmploymentNotConfirmedException();
        }
        return new MemberVerification(provider, role, photo, confirmation, Objects.requireNonNull(now));
    }

    /**
     * Records the KYC provider's result. Verified, the person is ready to add, with the registered name and
     * verified-until date from the result. Rejected, or timed out at the KYC provider, the verification has
     * ended and nothing is added. A repeated result changes nothing.
     */
    public VerificationOutcome recordKycResult(KycResult result, Instant now) {
        if (status != MemberVerificationStatus.WAITING) {
            return repeatedResult(result);
        }
        if (!result.isVerified()) {
            status = MemberVerificationStatus.REJECTED;
            return VerificationOutcome.REJECTED;
        }
        registeredName = result.registeredName().orElseThrow();
        verifiedUntil = result.verifiedUntil().orElseThrow();
        verifiedAt = now;
        status = MemberVerificationStatus.VERIFIED;
        return VerificationOutcome.VERIFIED;
    }

    /** Whether it ended without adding anyone: the verification was rejected, and the provider can start again. */
    public boolean hasEnded() {
        return status == MemberVerificationStatus.REJECTED;
    }

    void markAdded() {
        status = MemberVerificationStatus.ADDED;
    }

    public String id() {
        return id;
    }

    public String providerId() {
        return providerId;
    }

    public TeamRole role() {
        return role;
    }

    public Photo photo() {
        return photo;
    }

    public EmploymentConfirmation confirmation() {
        return confirmation;
    }

    /** When the provider sent the add form. */
    public Instant startedAt() {
        return startedAt;
    }

    public MemberVerificationStatus status() {
        return status;
    }

    /** The name on the person's identity document, from the KYC result; empty until verified. */
    public Optional<String> registeredName() {
        return Optional.ofNullable(registeredName);
    }

    /** Until when the person is verified; empty until verified. */
    public Optional<Instant> verifiedUntil() {
        return Optional.ofNullable(verifiedUntil);
    }

    /** When the person passed; empty until verified. */
    public Optional<Instant> verifiedAt() {
        return Optional.ofNullable(verifiedAt);
    }

    private VerificationOutcome repeatedResult(KycResult result) {
        if (!isSameAsRecorded(result)) {
            throw new VerificationNotWaitingException(id, status);
        }
        return VerificationOutcome.UNCHANGED;
    }

    private boolean isSameAsRecorded(KycResult result) {
        if (result.isVerified()) {
            return status == MemberVerificationStatus.VERIFIED || status == MemberVerificationStatus.ADDED;
        }
        return status == MemberVerificationStatus.REJECTED;
    }
}
