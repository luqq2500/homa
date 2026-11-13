package verification.domain;

/** Why a person's check added nothing; the provider is told which and can start again. */
public enum CheckFailure {
    REJECTED,
    PHOTO_MISMATCH,
    UNDER_18,
    TIMED_OUT
}
