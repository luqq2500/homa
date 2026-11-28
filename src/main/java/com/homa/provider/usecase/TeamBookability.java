package com.homa.provider.usecase;

import com.homa.ie.events.ProviderBookabilityChangedIE;
import com.homa.provider.domain.Bookability;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.Team;
import com.homa.provider.usecase.port.out.EventIds;
import com.homa.provider.usecase.port.out.ProviderIEPublisher;
import com.homa.provider.usecase.port.out.TeamMemberRepository;
import java.time.Instant;
import java.util.Objects;

/**
 * Re-checks whether a provider can be booked after its team changed (PRV-2), and tells Listing, Reservation and
 * Notification when that changed: a provider whose helpers are all deactivated disappears from search, and shows again
 * when one is back. The domain decides ({@link Provider#bookabilityFor}); this only compares before and after.
 */
final class TeamBookability {

    private final TeamMemberRepository members;
    private final ProviderIEPublisher publisher;
    private final EventIds eventIds;

    TeamBookability(TeamMemberRepository members, ProviderIEPublisher publisher, EventIds eventIds) {
        this.members = Objects.requireNonNull(members, "members");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.eventIds = Objects.requireNonNull(eventIds, "eventIds");
    }

    /** Whether the provider can be booked now, with the team as stored. */
    Bookability of(Provider provider) {
        return provider.bookabilityFor(new Team(members.findByProviderId(provider.id())).activeHelperCount());
    }

    /**
     * Publishes {@link ProviderBookabilityChangedIE} at {@code at} when the provider's bookability is no longer
     * {@code before}, and returns what it is now. A provider that hasn't gone live stays not live, so nothing is
     * published for it.
     */
    Bookability announceChangeFrom(Bookability before, Provider provider, Instant at) {
        Bookability after = of(provider);
        if (after != before) {
            publisher.publish(new ProviderBookabilityChangedIE(eventIds.next(), provider.id().value(),
                    after.isBookable(), after.isBookable() ? null : after.name(), at));
        }
        return after;
    }
}
