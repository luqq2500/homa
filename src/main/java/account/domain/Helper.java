package account.domain;

import account.domain.exception.HelperPhotoRequiredException;
import account.domain.exception.HelperRemovedException;

/**
 * A helper on a provider's team. Helpers have no accounts of their own: the provider adds them, and
 * each verifies through an eKYC link the provider sends. Customers see each helper's name and photo.
 */
public final class Helper {

    private final String id;
    private final String providerId;
    private final String name;
    private final Photo photo;
    private VerificationStatus ekyc = VerificationStatus.PENDING;
    private boolean removed;

    private Helper(String id, String providerId, String name, Photo photo) {
        this.id = id;
        this.providerId = providerId;
        this.name = name;
        this.photo = photo;
    }

    public static Helper add(String providerId, String name, Photo photo) {
        String provider = AccountText.require(providerId, "A helper's provider");
        String helperName = AccountText.require(name, "A helper's name");
        if (photo == null) {
            throw new HelperPhotoRequiredException();
        }
        return new Helper(new HelperId().generate(), provider, helperName, photo);
    }

    public void recordEkycResult(VerificationStatus result) {
        requireOnTeam();
        ekyc = EkycResult.require(result);
    }

    /** Takes the helper off the provider's team. A removed helper stays on record but cannot be named for jobs. */
    public void remove() {
        requireOnTeam();
        removed = true;
    }

    /** Whether the provider can name this helper for a reservation: on the team and verified. */
    public boolean isVerifiedTeamMember() {
        return !removed && ekyc == VerificationStatus.VERIFIED;
    }

    public boolean isOnTeam() {
        return !removed;
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

    public VerificationStatus ekycStatus() {
        return ekyc;
    }

    private void requireOnTeam() {
        if (removed) {
            throw new HelperRemovedException(id);
        }
    }
}
