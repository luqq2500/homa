package account.domain;

/**
 * Where a team member stands. A person exists as a team member only once their eKYC is verified, so there
 * is no invited state; before that they are a {@link MemberVerification}.
 */
public enum TeamStatus {
    /** On the team: shown to customers, counted in the team, and can be named for jobs. */
    MEMBER,
    /** Re-verification was missed: off the team until they pass eKYC again. */
    LAPSED,
    /** Removed by the provider, or by Homa. Cannot come back. */
    REMOVED
}
