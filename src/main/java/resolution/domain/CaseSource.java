package resolution.domain;

/** Why a case is in the founders' queue. */
public enum CaseSource {
    CUSTOMER_REPORT,
    PROVIDER_REPORT,
    NO_SHOW_CHALLENGE,
    MISSED_COMPLETION
}
