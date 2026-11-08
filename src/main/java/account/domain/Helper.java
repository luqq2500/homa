package account.domain;

import account.domain.exception.HelperAlreadyTeamMemberException;
import account.domain.exception.HelperPhotoRequiredException;
import account.domain.exception.HelperNotTeamMemberException;
import account.domain.exception.HelperRemovedException;
import account.domain.exception.ReverificationNotYetDueException;
import java.time.Instant;
import java.util.Optional;

/**
 * A helper a provider has invited to their team. Helpers have no accounts of their own: the provider
 * adds their name and photo and sends them an eKYC link. A helper becomes a team member only once their
 * eKYC is verified. Until then they are not on the team, not shown to customers, not counted in the team
 * size and cannot be named for jobs.
 *
 * <p>A team member is re-verified every re-verification period. One who misses their due date lapses and
 * leaves the team until they pass.
 */
public final class Helper {

    private final String id;
    private final String providerId;
    private final String name;
    private final Photo photo;
    private TeamStatus status = TeamStatus.INVITED;
    private Verification verification;

    private Helper(String id, String providerId, String name, Photo photo) {
        this.id = id;
        this.providerId = providerId;
        this.name = name;
        this.photo = photo;
    }

    /** The provider invites a helper, with a name and a photo. The helper starts {@code INVITED}. */
    public static Helper invite(String providerId, String name, Photo photo) {
        String provider = AccountText.require(providerId, "A helper's provider");
        String helperName = AccountText.require(name, "A helper's name");
        if (photo == null) {
            throw new HelperPhotoRequiredException();
        }
        return new Helper(new HelperId().generate(), provider, helperName, photo);
    }

    /**
     * Records Kenal's result.
     *
     * <ul>
     *   <li>An invite: verified, the helper joins the team as a {@code MEMBER}; rejected, the invite is
     *       {@code INVITE_REJECTED} and the helper can redo eKYC.
     *   <li>A member with a re-check open: verified starts a new period; rejected changes nothing yet,
     *       and the helper can retry until the due date.
     *   <li>A member with no re-check open: a repeated verified result changes nothing; a rejection is
     *       refused, so a late result cannot drop a member off the team.
     *   <li>A lapsed helper: verified makes them a {@code MEMBER} again with a new period; rejected keeps
     *       them lapsed.
     * </ul>
     */
    public EkycOutcome recordEkycResult(VerificationStatus result, VerificationLimits limits, Instant now) {
        requireNotRemoved();
        boolean verified = EkycResult.isVerified(result);
        return switch (status) {
            case MEMBER -> recordMemberResult(verified, limits, now);
            case LAPSED -> recordRecheckResult(verified, limits, now);
            default -> recordInviteResult(verified, limits, now);
        };
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

    /** When the helper was last verified and when re-verification is due; empty until they join the team. */
    public Optional<Verification> verification() {
        return Optional.ofNullable(verification);
    }

    /** The provider withdraws an invite or removes a team member. A removed helper cannot come back. */
    public void remove() {
        requireNotRemoved();
        status = TeamStatus.REMOVED;
    }

    /** Whether the helper is on the team: true only for a {@code MEMBER}. */
    public boolean isTeamMember() {
        return status == TeamStatus.MEMBER;
    }

    public TeamStatus teamStatus() {
        return status;
    }

    public String id() {
        return id;
    }

    public String providerId() {
        return providerId;
    }

    public String name() {
        return name;
    }

    public Photo photo() {
        return photo;
    }

    private EkycOutcome recordInviteResult(boolean verified, VerificationLimits limits, Instant now) {
        if (!verified) {
            status = TeamStatus.INVITE_REJECTED;
            return EkycOutcome.REJECTED;
        }
        join(limits, now);
        return EkycOutcome.VERIFIED;
    }

    private EkycOutcome recordMemberResult(boolean verified, VerificationLimits limits, Instant now) {
        if (!verification.recheckOpen()) {
            if (!verified) {
                throw new HelperAlreadyTeamMemberException(id);
            }
            return EkycOutcome.UNCHANGED;
        }
        return recordRecheckResult(verified, limits, now);
    }

    private EkycOutcome recordRecheckResult(boolean verified, VerificationLimits limits, Instant now) {
        if (!verified) {
            return EkycOutcome.RECHECK_FAILED;
        }
        join(limits, now);
        return EkycOutcome.REVERIFIED;
    }

    private void join(VerificationLimits limits, Instant now) {
        status = TeamStatus.MEMBER;
        verification = Verification.passedAt(now, limits);
    }

    private Verification memberVerification() {
        requireNotRemoved();
        if (status != TeamStatus.MEMBER) {
            throw new HelperNotTeamMemberException(id, status);
        }
        return verification;
    }

    private void requireNotRemoved() {
        if (status == TeamStatus.REMOVED) {
            throw new HelperRemovedException(id);
        }
    }
}
