package com.homa.rating.domain;

import java.util.Objects;

/**
 * A customer or a provider, the one who writes a rating (RTG-1) or whose ratings and reliability are shown
 * (RTG-6, RTG-10). The id is the customer's or the provider's own id.
 */
public record Party(PartyRole role, String id) {

    public Party {
        Objects.requireNonNull(role, "role");
        Ids.requireText(id, "Party id");
    }

    public static Party customer(CustomerId customerId) {
        return new Party(PartyRole.CUSTOMER, customerId.value());
    }

    public static Party provider(ProviderId providerId) {
        return new Party(PartyRole.PROVIDER, providerId.value());
    }

    /**
     * RTG-6, RTG-10: a party's ratings and reliability are seen by the party itself and by the other side: a
     * provider's by customers, a customer's by providers. Another provider doesn't see a provider's, nor another
     * customer a customer's.
     */
    public boolean canBeSeenBy(Party viewer) {
        Objects.requireNonNull(viewer, "viewer");
        return equals(viewer) || role != viewer.role;
    }
}
