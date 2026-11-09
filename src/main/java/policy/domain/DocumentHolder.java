package policy.domain;

/** Who verifies with a document, for the accepted documents list. */
public enum DocumentHolder {
    /** An independent provider, or a business provider's owner or director. */
    PROVIDER,
    INDEPENDENT_TEAM_MEMBER,
    BUSINESS_TEAM_MEMBER
}
