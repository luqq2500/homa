package resolution.domain;

/** Why a case is in the admins' queue. */
public enum CaseSource {
    CUSTOMER_REPORT,
    PROVIDER_REPORT,
    NO_SHOW_CHALLENGE,
    MISSED_COMPLETION,
    /** The customer disputed a card payment with their bank after the job; what the bank took back is at stake. */
    CHARGEBACK
}
