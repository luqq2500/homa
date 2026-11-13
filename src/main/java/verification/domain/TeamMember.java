package verification.domain;

import java.time.Instant;
import java.util.Optional;
import verification.domain.exception.ProfilePhotoMissingException;
import verification.domain.exception.ReasonMissingException;
import verification.domain.exception.TeamMemberNotAwaitingConfirmationException;
import verification.domain.exception.TeamMemberNotAwaitingVerificationException;
import verification.domain.exception.TeamMemberNotOnTeamException;

/**
 * A person a provider adds to their team. Nothing is on the team until the provider has started,
 * the person has verified on the spot, and the provider has confirmed their employment.
 */
public final class TeamMember {

    private final String id;
    private final String businessId;
    private final String profilePhoto;
    private Role role;
    private TeamMemberStatus status;
    private String registeredName;
    private String documentCountry;
    private Instant verifiedUntil;
    private Instant lastVerifiedAt;
    private CheckFailure checkFailure;
    private EmploymentConfirmation confirmation;
    private Removal removal;

    private TeamMember(String id, String businessId, String profilePhoto, Role role, TeamMemberStatus status) {
        this.id = id;
        this.businessId = businessId;
        this.profilePhoto = profilePhoto;
        this.role = role;
        this.status = status;
    }

    /** A stored person, with everything recorded about them. */
    public static TeamMember stored(String id, String businessId, String profilePhoto, Role role,
                                    TeamMemberStatus status, String registeredName, String documentCountry,
                                    Instant verifiedUntil, Instant lastVerifiedAt, CheckFailure checkFailure,
                                    EmploymentConfirmation confirmation, Removal removal) {
        TeamMember member = new TeamMember(id, businessId, profilePhoto, role, status);
        member.registeredName = registeredName;
        member.documentCountry = documentCountry;
        member.verifiedUntil = verifiedUntil;
        member.lastVerifiedAt = lastVerifiedAt;
        member.checkFailure = checkFailure;
        member.confirmation = confirmation;
        member.removal = removal;
        return member;
    }

    /** The provider starts adding a person, with their role and the photo customers will see. */
    public static TeamMember start(Business business, Role role, String profilePhoto) {
        business.requireVerified();
        if (profilePhoto == null || profilePhoto.isBlank()) {
            throw new ProfilePhotoMissingException();
        }
        return new TeamMember(new TeamMemberId().generate(), business.id(), profilePhoto, role,
                TeamMemberStatus.WAITING_FOR_VERIFICATION);
    }

    /** The person passed their check: their name is the one on their identity document. */
    public void verified(String registeredName, String documentCountry, Instant verifiedUntil, Instant now) {
        requireWaitingForVerification();
        this.registeredName = registeredName;
        this.documentCountry = documentCountry;
        this.verifiedUntil = verifiedUntil;
        this.lastVerifiedAt = now;
        this.status = TeamMemberStatus.WAITING_FOR_CONFIRMATION;
    }

    /** The check was rejected, the photo did not match, the person is under 18, or the session timed out. */
    public void checkFailed(CheckFailure failure) {
        requireWaitingForVerification();
        this.checkFailure = failure;
        this.status = TeamMemberStatus.CHECK_FAILED;
    }

    /** The provider confirms this named person's employment; only then do they join the team. */
    public void confirm(boolean lawfullyEmployed, boolean holdsValidWorkPass, String termsVersion, Instant now) {
        if (status != TeamMemberStatus.WAITING_FOR_CONFIRMATION) {
            throw new TeamMemberNotAwaitingConfirmationException(id);
        }
        this.confirmation =
                EmploymentConfirmation.of(documentCountry, lawfullyEmployed, holdsValidWorkPass, termsVersion, now);
        this.status = TeamMemberStatus.ON_TEAM;
    }

    /** Changes between helper and supervisor, without verifying again. Returns whether the role changed. */
    public boolean changeRole(Role role) {
        requireJoined();
        boolean changed = this.role != role;
        this.role = role;
        return changed;
    }

    /** A re-verification passed: the date moves out. Returns whether a lapsed person is back on the team. */
    public boolean reverified(Instant verifiedUntil, Instant now) {
        requireJoined();
        boolean restored = status == TeamMemberStatus.LAPSED;
        this.verifiedUntil = verifiedUntil;
        this.lastVerifiedAt = now;
        this.status = TeamMemberStatus.ON_TEAM;
        return restored;
    }

    /** Re-verification is needed by a date: verified-until moves to it if it is sooner. Returns whether it moved. */
    public boolean reverificationNeededBy(Instant date) {
        requireJoined();
        if (!date.isBefore(verifiedUntil)) {
            return false;
        }
        verifiedUntil = date;
        return true;
    }

    /** The person missed a re-verification or was seriously flagged. Returns whether they left the team just now. */
    public boolean lapse() {
        if (status != TeamMemberStatus.ON_TEAM) {
            return false;
        }
        status = TeamMemberStatus.LAPSED;
        return true;
    }

    /** The provider removes the person. Returns whether they were on the team until now. */
    public boolean removeByProvider(Instant now) {
        return remove(new Removal(businessId, null, now));
    }

    /** Homa removes a person it learned is not allowed to work, and records what happened. */
    public boolean removeByHoma(String adminId, String reason, Instant now) {
        if (reason == null || reason.isBlank()) {
            throw new ReasonMissingException();
        }
        return remove(new Removal(adminId, reason, now));
    }

    private boolean remove(Removal removal) {
        if (status == TeamMemberStatus.REMOVED || status == TeamMemberStatus.CHECK_FAILED) {
            throw new TeamMemberNotOnTeamException(id);
        }
        boolean wasOnTeam = isOnTeam();
        this.removal = removal;
        this.status = TeamMemberStatus.REMOVED;
        return wasOnTeam;
    }

    private void requireWaitingForVerification() {
        if (status != TeamMemberStatus.WAITING_FOR_VERIFICATION) {
            throw new TeamMemberNotAwaitingVerificationException(id);
        }
    }

    /** On the team or lapsed: someone who joined and has not been removed. */
    private void requireJoined() {
        if (status != TeamMemberStatus.ON_TEAM && status != TeamMemberStatus.LAPSED) {
            throw new TeamMemberNotOnTeamException(id);
        }
    }

    public boolean isOnTeam() {
        return status == TeamMemberStatus.ON_TEAM;
    }

    public String id() {
        return id;
    }

    public String businessId() {
        return businessId;
    }

    public String profilePhoto() {
        return profilePhoto;
    }

    public Role role() {
        return role;
    }

    public TeamMemberStatus status() {
        return status;
    }

    public String registeredName() {
        return registeredName;
    }

    public String documentCountry() {
        return documentCountry;
    }

    public Instant verifiedUntil() {
        return verifiedUntil;
    }

    public Instant lastVerifiedAt() {
        return lastVerifiedAt;
    }

    public Optional<CheckFailure> checkFailure() {
        return Optional.ofNullable(checkFailure);
    }

    public Optional<EmploymentConfirmation> confirmation() {
        return Optional.ofNullable(confirmation);
    }

    public Optional<Removal> removal() {
        return Optional.ofNullable(removal);
    }
}
