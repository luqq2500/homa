package account.domain;

import account.domain.exception.HelperAlreadyTeamMemberException;
import account.domain.exception.HelperPhotoRequiredException;
import account.domain.exception.HelperRemovedException;

/**
 * A helper a provider has invited to their team. Helpers have no accounts of their own: the provider
 * adds their name and photo and sends them an eKYC link. A helper becomes a team member only once their
 * eKYC is verified. Until then they are not on the team, not shown to customers, not counted in the team
 * size and cannot be named for jobs.
 */
public final class Helper {

    private final String id;
    private final String providerId;
    private final String name;
    private final Photo photo;
    private TeamStatus status = TeamStatus.INVITED;

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
     * Records Kenal's result for an invite. Verified, the helper joins the team as a {@code MEMBER};
     * rejected, the invite is {@code INVITE_REJECTED} and the helper can redo eKYC.
     */
    public void recordEkycResult(VerificationStatus result) {
        if (status == TeamStatus.REMOVED) {
            throw new HelperRemovedException(id);
        }
        if (status == TeamStatus.MEMBER) {
            throw new HelperAlreadyTeamMemberException(id);
        }
        boolean verified = EkycResult.require(result) == VerificationStatus.VERIFIED;
        status = verified ? TeamStatus.MEMBER : TeamStatus.INVITE_REJECTED;
    }

    /** The provider withdraws an invite or removes a team member. A removed helper cannot come back. */
    public void remove() {
        if (status == TeamStatus.REMOVED) {
            throw new HelperRemovedException(id);
        }
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
}
