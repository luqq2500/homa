package offering.domain;

/** Why a live provider stopped being live. */
public enum StopReason {
    NO_JOB,
    NO_HOURS,
    NO_HELPER,
    NO_SUPERVISOR,
    BUSINESS_LAPSED
}
