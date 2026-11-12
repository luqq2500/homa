package account.domain;

/** Where adding a team member stands. Only a verified one can be added, once. */
public enum MemberVerificationStatus {
    /** The person is doing the KYC provider's check on the spot on the provider's phone; shown to the provider as waiting for verification. */
    WAITING,
    /** The KYC provider verified the person: ready to add. */
    VERIFIED,
    /** The KYC provider rejected the check, or the session timed out: ended, nothing is added, and the provider can start again. */
    REJECTED,
    /** The team member was added from it. */
    ADDED
}
