package verification.domain;

/** Where a business stands with its KYB. */
public enum BusinessStatus {
    /** Registered; KYB not passed yet. */
    NOT_VERIFIED,
    /** KYB rejected; the business is told to redo it. */
    REJECTED,
    VERIFIED,
    /** Missed a re-verification: the listing is paused until it passes. */
    LAPSED,
    /** The KYC provider confirmed a serious flag. */
    SERIOUSLY_FLAGGED
}
