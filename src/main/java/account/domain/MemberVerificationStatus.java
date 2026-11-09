package account.domain;

/** Where adding a team member stands. Only a verified one can be added, once. */
public enum MemberVerificationStatus {
    /** The eKYC link is out; shown to the provider as waiting for verification. */
    WAITING,
    /** The eKYC passed: ready to add. */
    VERIFIED,
    /** The eKYC was rejected: ended, nothing is added, and the provider can start again. */
    REJECTED,
    /** Not finished within the verification link expiry: ended, and the provider can start again. */
    EXPIRED,
    /** The team member was added from it. */
    ADDED
}
