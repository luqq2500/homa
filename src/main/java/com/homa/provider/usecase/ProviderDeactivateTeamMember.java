package com.homa.provider.usecase;

import com.homa.provider.domain.Bookability;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.usecase.dto.DeactivateTeamMemberRequest;
import com.homa.provider.usecase.dto.DeactivateTeamMemberResponse;
import com.homa.provider.usecase.port.in.TeamMemberDeactivator;
import com.homa.provider.usecase.port.out.EventIds;
import com.homa.provider.usecase.port.out.ProviderIEPublisher;
import com.homa.provider.usecase.port.out.ProviderRepository;
import com.homa.provider.usecase.port.out.TeamMemberRepository;
import com.homa.provider.usecase.port.out.TimeSource;
import java.time.Instant;
import java.util.Objects;

/**
 * The owner deactivates a member, for example for two weeks' leave (HLP-5). A deactivated member isn't active, so
 * can't be rostered (HLP-6): {@code TeamMemberDeactivatedIE} with reason DEACTIVATED tells Reservation, which asks the
 * provider to replace them on an upcoming roster (ROS-5), and Listing, which counts one active helper fewer. When that
 * leaves a live provider without an active helper, it can't be booked and disappears from search, so
 * {@code ProviderBookabilityChangedIE} says so (PRV-2). Only the owner acts on the team (PRV-1).
 */
public final class ProviderDeactivateTeamMember implements TeamMemberDeactivator {

    private final ProviderRepository providers;
    private final TeamMemberRepository members;
    private final ProviderIEPublisher publisher;
    private final TimeSource time;
    private final EventIds eventIds;
    private final TeamBookability bookability;

    public ProviderDeactivateTeamMember(ProviderRepository providers, TeamMemberRepository members,
            ProviderIEPublisher publisher, TimeSource time, EventIds eventIds) {
        this.providers = Objects.requireNonNull(providers, "providers");
        this.members = Objects.requireNonNull(members, "members");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.time = Objects.requireNonNull(time, "time");
        this.eventIds = Objects.requireNonNull(eventIds, "eventIds");
        this.bookability = new TeamBookability(members, publisher, eventIds);
    }

    @Override
    public DeactivateTeamMemberResponse deactivate(DeactivateTeamMemberRequest request) {
        Provider provider = OwnerAuthorization.providerRunBy(providers, request.providerId(), request.actingPersonId());
        TeamMember member = OwnerAuthorization.memberOf(members, provider, request.memberId());
        Instant now = time.now();
        Bookability before = bookability.of(provider);
        member.deactivate();
        members.save(member);
        publisher.publish(TeamMemberEvents.deactivated(eventIds.next(), member, TeamMemberEvents.DEACTIVATED, now));
        Bookability after = bookability.announceChangeFrom(before, provider, now);
        return new DeactivateTeamMemberResponse(
                member.id().value(), provider.id().value(), member.personId().value(), after.isBookable());
    }
}
