package account.domain;

/** What a result from the KYC provider did. Each outcome but {@code UNCHANGED} has its own integration event. */
public enum VerificationOutcome {
    /** First verification passed: a business is verified, or a person being added is ready to join the team. */
    VERIFIED,
    /** First verification failed: the business redoes it, or nothing is added. */
    REJECTED,
    /** Passed again, with a new verified-until date; a lapsed business or team member is back. */
    REVERIFIED,
    /** A re-verification failed: nothing changes, and the KYC provider decides when it lapses. */
    RECHECK_FAILED,
    /** The same verified result again, as a provider may deliver a result twice. */
    UNCHANGED
}
