package offering.domain;

/** What taking a retired job off a listing did, so the provider can be told. */
public enum JobWithdrawal {
    /** The listing did not offer the job; nothing changed. */
    NOT_OFFERED,
    /** The job came off; the listing is as live, paused or draft as it was. */
    WITHDRAWN,
    /** The job came off and it was the listing's last job, so a live listing is no longer live. */
    WITHDRAWN_AND_OFFLINE
}
