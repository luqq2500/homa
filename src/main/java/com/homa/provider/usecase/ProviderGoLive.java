package com.homa.provider.usecase;

import com.homa.ie.events.ProviderWentLiveIE;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.Team;
import com.homa.provider.usecase.dto.GoLiveRequest;
import com.homa.provider.usecase.dto.GoLiveResponse;
import com.homa.provider.usecase.port.in.ProviderLauncher;
import com.homa.provider.usecase.port.out.EventIds;
import com.homa.provider.usecase.port.out.ProviderIEPublisher;
import com.homa.provider.usecase.port.out.ProviderRepository;
import com.homa.provider.usecase.port.out.TeamMemberRepository;
import com.homa.provider.usecase.port.out.TimeSource;
import java.util.Objects;

/**
 * The owner takes the provider live. The domain needs a verified payout account in the provider's name and at least
 * one active helper; supervisors don't count (PRV-2, PRV-3). {@code ProviderWentLiveIE} tells Listing, Reservation,
 * Payment and Rating. Its business registration number stays null until Business status exists (PRV-7, slice 6).
 */
public final class ProviderGoLive implements ProviderLauncher {

    private final ProviderRepository providers;
    private final TeamMemberRepository members;
    private final ProviderIEPublisher publisher;
    private final TimeSource time;
    private final EventIds eventIds;

    public ProviderGoLive(ProviderRepository providers, TeamMemberRepository members, ProviderIEPublisher publisher,
            TimeSource time, EventIds eventIds) {
        this.providers = Objects.requireNonNull(providers, "providers");
        this.members = Objects.requireNonNull(members, "members");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.time = Objects.requireNonNull(time, "time");
        this.eventIds = Objects.requireNonNull(eventIds, "eventIds");
    }

    @Override
    public GoLiveResponse goLive(GoLiveRequest request) {
        Provider provider = OwnerAuthorization.providerRunBy(providers, request.providerId(), request.actingPersonId());
        provider.goLive(new Team(members.findByProviderId(provider.id())).activeHelperCount());
        providers.save(provider);
        publisher.publish(new ProviderWentLiveIE(
                eventIds.next(), provider.id().value(), provider.owner().personId().value(), null, time.now()));
        return new GoLiveResponse(provider.id().value(), provider.status().name());
    }
}
