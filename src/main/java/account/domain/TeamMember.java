package account.domain;

import account.domain.exception.MemberAlreadyAddedException;
import account.domain.exception.MemberNotVerifiedException;
import account.domain.exception.NotATeamMemberException;
import account.domain.exception.TeamMemberRemovedException;
import java.time.Instant;

/**
 * A person verified to work for a business, as a helper or a supervisor. Team members have no accounts of
 * their own. A team member exists only from a verified {@link MemberVerification}, with the registered name
 * from the KYC result; nobody types or changes it. The KYC provider re-verifies them on its own schedule
 * and reports a missed re-verification, which takes them off the team until they pass.
 */
public final class TeamMember {

    private final String id;
    private final String providerId;
    private final TeamRole role;
    private final Photo photo;
    private final EmploymentConfirmation confirmation;
    private final Instant addedAt;
    private String registeredName;
    private Instant verifiedUntil;
    private Instant lastVerifiedAt;
    private TeamStatus status = TeamStatus.MEMBER;

    private TeamMember(MemberVerification verified, Instant now) {
        this.id = new TeamMemberId().generate();
        this.providerId = verified.providerId();
        this.role = verified.role();
        this.photo = verified.photo();
        this.confirmation = verified.confirmation();
        this.addedAt = now;
        this.registeredName = verified.registeredName().orElseThrow();
        this.verifiedUntil = verified.verifiedUntil().orElseThrow();
        this.lastVerifiedAt = verified.verifiedAt().orElseThrow();
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
     * Records the KYC provider's re-verification result. A pass keeps the member verified until the new
     * date, and brings a lapsed member back onto the team; a repeated pass changes nothing. A failed
     * re-verification changes nothing: the KYC provider decides when the member lapses.
     */
    public VerificationOutcome recordKycResult(KycResult result, Instant now) {
        requireNotRemoved();
        if (!result.isVerified()) {
            return VerificationOutcome.RECHECK_FAILED;
        }
        Instant until = result.verifiedUntil().orElseThrow();
        if (status == TeamStatus.MEMBER && until.equals(verifiedUntil)) {
            return VerificationOutcome.UNCHANGED;
        }
        registeredName = result.registeredName().orElseThrow();
        verifiedUntil = until;
        lastVerifiedAt = now;
        status = TeamStatus.MEMBER;
        return VerificationOutcome.REVERIFIED;
    }

    /** The KYC provider reports a missed re-verification: the member leaves the team until they pass. */
    public void lapse() {
        requireNotRemoved();
        if (status != TeamStatus.MEMBER) {
            throw new NotATeamMemberException(id, status);
        }
        status = TeamStatus.LAPSED;
    }

    /** The provider removes the team member, or Homa does. A removed team member cannot come back. */
    public void remove() {
        requireNotRemoved();
        status = TeamStatus.REMOVED;
    }

    /** Whether the person is on the team: true only for a {@code MEMBER}. */
    public boolean isTeamMember() {
        return status == TeamStatus.MEMBER;
    }

    public TeamStatus teamStatus() {
        return status;
    }

    /** The name on their identity document, from the KYC result. */
    public String registeredName() {
        return registeredName;
    }

    public TeamRole role() {
        return role;
    }

    /** Until when the member is verified. */
    public Instant verifiedUntil() {
        return verifiedUntil;
    }

    /** When the member last passed, as the Verified ID badge shows it. */
    public Instant lastVerifiedAt() {
        return lastVerifiedAt;
    }

    public String id() {
        return id;
    }

    public String providerId() {
        return providerId;
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

    private void requireNotRemoved() {
        if (status == TeamStatus.REMOVED) {
            throw new TeamMemberRemovedException(id);
        }
    }
}
