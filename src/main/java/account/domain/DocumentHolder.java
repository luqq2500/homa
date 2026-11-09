package account.domain;

/** Who verifies with a document, for the accepted documents list. */
public enum DocumentHolder {
    /** An independent provider, or a business provider's owner or director. */
    PROVIDER,
    INDEPENDENT_TEAM_MEMBER,
    BUSINESS_TEAM_MEMBER;

    /** Which list a team member of a provider of {@code type} is checked against. */
    public static DocumentHolder teamMemberOf(ProviderType type) {
        return type == ProviderType.BUSINESS ? BUSINESS_TEAM_MEMBER : INDEPENDENT_TEAM_MEMBER;
    }
}
