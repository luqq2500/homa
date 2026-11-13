package verification.domain;

/** Where a person stands on the way to the team, and after. */
public enum TeamMemberStatus {
    WAITING_FOR_VERIFICATION,
    WAITING_FOR_CONFIRMATION,
    ON_TEAM,
    /** Missed a re-verification or was seriously flagged: off the team until they pass. */
    LAPSED,
    REMOVED,
    /** The check failed, so nothing was added. */
    CHECK_FAILED
}
