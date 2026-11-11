package account.domain;

/**
 * Where a team member stands. A person exists as a team member only once the KYC provider has verified
 * them; before that they are a {@link MemberVerification}.
 */
public enum TeamStatus {
    /** On the team: shown to customers, counted in the team, and can be named for jobs. */
    MEMBER,
    /** The KYC provider reports a missed re-verification: off the team until they pass again. */
    LAPSED,
    /** Removed by the provider, or by Homa. Cannot come back. */
    REMOVED
}
