package resolution.domain;

/** Whether a case is waiting for the admins, has been decided, or was closed without a decision. */
public enum CaseStatus {
    OPEN,
    DECIDED,
    /** A seriously flagged provider's reservation was taken over by the flag, so the case closed with no decision. */
    CLOSED
}
