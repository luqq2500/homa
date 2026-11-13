package reservation.domain;

/** Why a reservation is Disputed and waiting for Homa's review. */
public enum DisputeCause {
    /** A report was filed. */
    REPORT,
    /** The provider missed the completion deadline. */
    MISSED_COMPLETION_DEADLINE,
    /** The customer challenged a no-show. It is reviewed on the challenge alone and takes no reports. */
    NO_SHOW_CHALLENGE,
    /** The provider's business was seriously flagged. */
    SERIOUS_FLAG
}
