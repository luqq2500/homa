package reservation.domain;

/** The result of a named person's live selfie at the door. Homa keeps only the result and time, never the selfie. */
public enum FaceCheckResult {
    PASSED,
    /** Failed after the attempts the KYC provider allows: the person cannot work on this job. */
    FAILED,
    /** The face check service was unavailable: check-in goes ahead on location only. */
    UNAVAILABLE
}
