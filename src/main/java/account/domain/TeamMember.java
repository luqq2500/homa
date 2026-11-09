package account.domain;

import account.domain.exception.MemberAlreadyAddedException;
import account.domain.exception.MemberNotVerifiedException;
import account.domain.exception.NotATeamMemberException;
import account.domain.exception.PhotoMatchAlreadyRecordedException;
import account.domain.exception.ReverificationNotYetDueException;
import account.domain.exception.TeamMemberAlreadyVerifiedException;
import account.domain.exception.TeamMemberRemovedException;
import java.time.Instant;
import java.util.Optional;

/**
 * A person verified to work for a provider, as a helper or a supervisor. Team members have no accounts of
 * their own. A team member exists only from a verified {@link MemberVerification}, with the registered name
 * from their eKYC; nobody types or changes it.
 *
 * <p>A team member is re-verified every re-verification period. One who misses their due date lapses and
 * leaves the team until they pass.
 */
public final class TeamMember {

    private final String id;
    private final String providerId;
    private final ProviderType providerType;
    private final TeamRole role;
    private final Photo photo;
    private final EmploymentConfirmation confirmation;
    private final Instant addedAt;
    private IdentityDocument document;
    private Verification verification;
    private TeamStatus status = TeamStatus.MEMBER;
    private Boolean photoMatches;

    private TeamMember(MemberVerification verified, Instant now) {
        this.id = new TeamMemberId().generate();
        this.providerId = verified.providerId();
        this.providerType = verified.providerType();
        this.role = verified.role();
        this.photo = verified.photo();
        this.confirmation = verified.confirmation();
        this.addedAt = now;
        this.document = verified.document().orElseThrow();
        this.verification = verified.verification().orElseThrow();
    }

    /** Adds the person from a verified verification. It is the only way a team member exists, and it is used once. */
    public static TeamMember add(MemberVerification verified, Instant now) {
        if (verified.status() == MemberVerificationStatus.ADDED) {
            throw new MemberAlreadyAddedException(verified.id());
        }
        if (verified.status() != MemberVerificationStatus.VERIFIED) {
            throw new MemberNotVerifiedException(verified.id(), verified.status());
        }
        TeamMember member = new TeamMember(verified, now);
        verified.markAdded();
        return member;
    }

    /**
     * Records Kenal's result for a re-verification.
     *
     * <ul>
     *   <li>A member with a re-check open: verified starts a new period, with the document read this time;
     *       rejected changes nothing yet, and the member can retry until the due date.
     *   <li>A member with no re-check open: a repeated verified result changes nothing; a rejection is
     *       refused, so a late result cannot drop a member off the team.
     *   <li>A lapsed member: verified makes them a {@code MEMBER} again with a new period; rejected keeps
     *       them lapsed.
     * </ul>
     *
     * A verified document must be on the accepted list for the provider's type.
     */
    public EkycOutcome recordEkycResult(EkycResult result, AcceptedDocuments accepted, VerificationLimits limits, Instant now) {
        requireNotRemoved();
        if (status == TeamStatus.MEMBER && !verification.recheckOpen()) {
            return repeatedResult(result);
        }
        if (!result.isVerified()) {
            return EkycOutcome.RECHECK_FAILED;
        }
        IdentityDocument read = result.verifiedDocument();
        DocumentRules.requireForTeamMember(providerType, read.type(), read.issuingCountry(), accepted);
        document = read;
        verification = Verification.passedAt(now, limits);
        status = TeamStatus.MEMBER;
        return EkycOutcome.REVERIFIED;
    }

    /** Opens the regular re-check, from the re-verification reminder before the due date. */
    public void openRecheck(VerificationLimits limits, Instant now) {
        verification = memberVerification().withRecheckOpened(id, limits, now);
    }

    /**
     * Homa asks for an early re-check, with a reason. The re-check opens now, and the due date moves to
     * the early re-check deadline if that is sooner; it never moves later.
     */
    public void requestEarlyRecheck(String reason, VerificationLimits limits, Instant now) {
        verification = memberVerification().withEarlyRecheck(reason, limits, now);
    }

    /** A member whose due date has come without passing re-verification lapses and leaves the team. */
    public void lapse(Instant now) {
        Verification current = memberVerification();
        if (!current.isDue(now)) {
            throw new ReverificationNotYetDueException(current.dueAt());
        }
        status = TeamStatus.LAPSED;
    }

    /** The provider removes the team member, or Homa does. A removed team member cannot come back. */
    public void remove() {
        requireNotRemoved();
        status = TeamStatus.REMOVED;
    }

    /** Records whether the profile photo matched the eKYC face, once, so the photo shows the verified person. */
    public void recordPhotoMatch(boolean matches) {
        if (photoMatches != null) {
            throw new PhotoMatchAlreadyRecordedException(id);
        }
        photoMatches = matches;
    }

    /** Whether the person is on the team: true only for a {@code MEMBER}. */
    public boolean isTeamMember() {
        return status == TeamStatus.MEMBER;
    }

    public TeamStatus teamStatus() {
        return status;
    }

    /** The name on their identity document, from the eKYC result. */
    public String registeredName() {
        return document.registeredName();
    }

    public IdentityDocument document() {
        return document;
    }

    public TeamRole role() {
        return role;
    }

    /** When the member was last verified and when re-verification is due. */
    public Verification verification() {
        return verification;
    }

    /** Whether the profile photo matched the eKYC face; empty until recorded. */
    public Optional<Boolean> photoMatch() {
        return Optional.ofNullable(photoMatches);
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

    public Photo photo() {
        return photo;
    }

    /** The lawful-employment confirmation the provider gave on the add form. */
    public EmploymentConfirmation employmentConfirmation() {
        return confirmation;
    }

    public Instant addedAt() {
        return addedAt;
    }

    private EkycOutcome repeatedResult(EkycResult result) {
        if (!result.isVerified()) {
            throw new TeamMemberAlreadyVerifiedException(id);
        }
        return EkycOutcome.UNCHANGED;
    }

    private Verification memberVerification() {
        requireNotRemoved();
        if (status != TeamStatus.MEMBER) {
            throw new NotATeamMemberException(id, status);
        }
        return verification;
    }

    private void requireNotRemoved() {
        if (status == TeamStatus.REMOVED) {
            throw new TeamMemberRemovedException(id);
        }
    }
}
