package account.domain;

/** How a provider is set up. Both can have a team of helpers and supervisors. */
public enum ProviderType {
    /** A Malaysian, not registered as a business. Team members are Malaysian; payouts go to a personal account. */
    INDEPENDENT,
    /** A business registered with SSM, verified through its owner or director. Team members can be foreign. */
    BUSINESS
}
