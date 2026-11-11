package reservation.domain;

/**
 * A named person's face check result from the KYC provider, which matches a live selfie against their
 * verification and allows its own number of attempts.
 */
public enum FaceCheckResult {
    PASSED,
    /** Failed every attempt the KYC provider allows: the person cannot work on this job. */
    FAILED
}
