package account.domain;

/** Why adding a team member ended without adding anyone, so the provider is told which. */
public enum MemberRejection {
    /** The KYC provider rejected the check, or the session timed out. */
    CHECK_REJECTED,
    /** The person is under 18, from the date of birth on their document. */
    UNDER_18,
    /** The profile photo isn't the same person as the verified selfie. */
    PHOTO_DOES_NOT_MATCH
}
