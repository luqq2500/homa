package account.domain;

import account.domain.exception.DocumentNotAcceptedException;
import account.domain.exception.EmploymentNotConfirmedException;
import account.domain.exception.PhotoRequiredException;
import account.domain.exception.VerificationNotExpiredException;
import account.domain.exception.VerificationNotWaitingException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Adding a team member, before the person exists on the team. The provider fills in one form: the role, the
 * document type, the issuing country (a business only), a profile photo, and the lawful-employment
 * confirmation for this person. The person then completes eKYC through a link. Until it is verified this is
 * shown to the provider as waiting for verification: not on the team, never shown to customers, and it
 * cannot be named for jobs.
 */
public final class MemberVerification {

    private final String id;
    private final String providerId;
    private final ProviderType providerType;
    private final TeamRole role;
    private final DocumentType documentType;
    private final Country issuingCountry;
    private final Photo photo;
    private final EmploymentConfirmation confirmation;
    private final VerificationLimits limits;
    private final Instant expiresAt;
    private MemberVerificationStatus status = MemberVerificationStatus.WAITING;
    private IdentityDocument document;
    private Verification verification;

    private MemberVerification(
            String providerId, ProviderType providerType, TeamRole role, DocumentType documentType, Country issuingCountry,
            Photo photo, EmploymentConfirmation confirmation, VerificationLimits limits, Instant now) {
        this.id = new MemberVerificationId().generate();
        this.providerId = providerId;
        this.providerType = providerType;
        this.role = role;
        this.documentType = documentType;
        this.issuingCountry = issuingCountry;
        this.photo = photo;
        this.confirmation = confirmation;
        this.limits = limits;
        this.expiresAt = now.plus(limits.linkExpiry());
    }

    /**
     * Starts adding a team member. Refused without the lawful-employment confirmation, with a country other
     * than Malaysia for an independent provider, or with a document outside the accepted list.
     */
    public static MemberVerification start(
            String providerId, ProviderType providerType, TeamRole role, DocumentType documentType, Country issuingCountry,
            Photo photo, EmploymentConfirmation confirmation, AcceptedDocuments accepted, VerificationLimits limits,
            Instant now) {
        String provider = AccountText.require(providerId, "A team member's provider");
        Objects.requireNonNull(role);
        if (photo == null) {
            throw new PhotoRequiredException();
        }
        if (confirmation == null) {
            throw new EmploymentNotConfirmedException();
        }
        DocumentRules.requireForTeamMember(providerType, documentType, issuingCountry, accepted);
        return new MemberVerification(
                provider, providerType, role, documentType, issuingCountry, photo, confirmation, limits, now);
    }

    /**
     * Records Kenal's result. Verified, the person is ready to add, with the registered name from the
     * document; the document must be the type and country chosen on the form, and still accepted. Rejected,
     * the verification has ended and nothing is added. A repeated result changes nothing.
     */
    public EkycOutcome recordEkycResult(EkycResult result, AcceptedDocuments accepted, Instant now) {
        if (status != MemberVerificationStatus.WAITING) {
            return repeatedResult(result);
        }
        if (!result.isVerified()) {
            status = MemberVerificationStatus.REJECTED;
            return EkycOutcome.REJECTED;
        }
        IdentityDocument read = result.verifiedDocument();
        requireDocumentAsChosen(read);
        DocumentRules.requireForTeamMember(providerType, read.type(), read.issuingCountry(), accepted);
        document = read;
        verification = Verification.passedAt(now, limits);
        status = MemberVerificationStatus.VERIFIED;
        return EkycOutcome.VERIFIED;
    }

    /** An eKYC not finished within the verification link expiry ends, and nothing is added. */
    public void expire(Instant now) {
        requireWaiting();
        if (now.isBefore(expiresAt)) {
            throw new VerificationNotExpiredException(expiresAt);
        }
        status = MemberVerificationStatus.EXPIRED;
    }

    /** Whether it ended without adding anyone, after a rejection or the link expiry. */
    public boolean hasEnded() {
        return status == MemberVerificationStatus.REJECTED || status == MemberVerificationStatus.EXPIRED;
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

    public ProviderType providerType() {
        return providerType;
    }

    public TeamRole role() {
        return role;
    }

    public DocumentType documentType() {
        return documentType;
    }

    public Country issuingCountry() {
        return issuingCountry;
    }

    public Photo photo() {
        return photo;
    }

    public EmploymentConfirmation confirmation() {
        return confirmation;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public MemberVerificationStatus status() {
        return status;
    }

    /** The document read by the eKYC; empty until verified. */
    public Optional<IdentityDocument> document() {
        return Optional.ofNullable(document);
    }

    /** When the person was verified and when re-verification is due; empty until verified. */
    public Optional<Verification> verification() {
        return Optional.ofNullable(verification);
    }

    private EkycOutcome repeatedResult(EkycResult result) {
        if (!isSameAsRecorded(result)) {
            throw new VerificationNotWaitingException(id, status);
        }
        return EkycOutcome.UNCHANGED;
    }

    private boolean isSameAsRecorded(EkycResult result) {
        if (result.isVerified()) {
            return status == MemberVerificationStatus.VERIFIED || status == MemberVerificationStatus.ADDED;
        }
        return status == MemberVerificationStatus.REJECTED;
    }

    private void requireDocumentAsChosen(IdentityDocument read) {
        if (read.type() != documentType || !read.issuingCountry().equals(issuingCountry)) {
            throw new DocumentNotAcceptedException(DocumentHolder.teamMemberOf(providerType), read.type(), read.issuingCountry());
        }
    }

    private void requireWaiting() {
        if (status != MemberVerificationStatus.WAITING) {
            throw new VerificationNotWaitingException(id, status);
        }
    }
}
