package com.homa.verification.domain;

/** Where a verification a person started stands. It is decided once, as verified or refused (IDN-2). */
public enum VerificationAttemptState {
    /** The verification service hasn't answered yet. */
    PENDING,
    /** The service passed the check and Homa verified the person for the role (IDN-1). */
    VERIFIED,
    /** Homa refused the verification under the rule it records, for example IDN-3 or CUS-10. */
    REFUSED
}
