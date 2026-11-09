package account.domain;

/** Where adding a team member stands. Only a verified one can be added, once. */
public enum MemberVerificationStatus {
    /** The person is doing eKYC on the spot on the provider's phone; shown to the provider as waiting for verification. */
    WAITING,
    /** The eKYC passed: ready to add. */
    VERIFIED,
    /** The eKYC was rejected: ended, nothing is added, and the provider can start again. */
    REJECTED,
    /** The team member was added from it. */
    ADDED
}
