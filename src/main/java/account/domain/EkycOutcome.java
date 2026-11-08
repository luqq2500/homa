package account.domain;

/** What a result from Kenal did. Each outcome but {@code UNCHANGED} has its own integration event. */
public enum EkycOutcome {
    /** First verification: a provider is verified, or an invited helper joins the team. */
    VERIFIED,
    /** First verification failed: the provider or invited helper must redo eKYC. */
    REJECTED,
    /** A re-check or a lapsed person passed: a new period starts, and a lapsed person is back. */
    REVERIFIED,
    /** A re-check or a lapsed person failed: nothing changes yet, and they can retry. */
    RECHECK_FAILED,
    /** A repeated verified result with no re-check open, as Kenal may deliver a result twice. */
    UNCHANGED
}
