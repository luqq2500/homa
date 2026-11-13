package offering.domain;

/** Whether customers can find and request a provider. */
public enum Listing {
    /** Still being set up; never live yet. */
    DRAFT,
    LIVE,
    /** Was live and stopped: the provider fixes what is missing and goes live again. */
    OFFLINE,
    /** The business lapsed: no new requests until it passes its re-verification, then it resumes by itself. */
    PAUSED
}
