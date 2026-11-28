package com.homa.provider.usecase;

import com.homa.provider.domain.Bookability;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.usecase.dto.ReactivateTeamMemberRequest;
import com.homa.provider.usecase.dto.ReactivateTeamMemberResponse;
import com.homa.provider.usecase.exception.PersonNotVerifiedException;
import com.homa.provider.usecase.port.in.TeamMemberReactivator;
import com.homa.provider.usecase.port.out.EventIds;
import com.homa.provider.usecase.port.out.ProviderIEPublisher;
import com.homa.provider.usecase.port.out.ProviderRepository;
import com.homa.provider.usecase.port.out.TeamMemberRepository;
import com.homa.provider.usecase.port.out.TimeSource;
import com.homa.provider.usecase.port.out.VerifiedPersonReplica;
import java.time.Instant;
import java.util.Objects;

/**
 * The owner reactivates a deactivated member when they're back (HLP-5). The domain refuses unless the member's
 * verification is still valid (HLP-7), and for a removed member (HLP-8). {@code TeamMemberActivatedIE} tells
 * Reservation and Listing, so the member can be rostered again, unless they are still not active for another reason
 * (suspension, slice 6). A live provider that had no active helper becomes bookable again, so
 * {@code ProviderBookabilityChangedIE} says so (PRV-2). Only the owner acts on the team (PRV-1).
 */
public final class ProviderReactivateTeamMember implements TeamMemberReactivator {

    private final ProviderRepository providers;
    private final TeamMemberRepository members;
    private final VerifiedPersonReplica verifiedPeople;
    private final ProviderIEPublisher publisher;
    private final TimeSource time;
    private final EventIds eventIds;
    private final TeamBookability bookability;

    public ProviderReactivateTeamMember(ProviderRepository providers, TeamMemberRepository members,
            VerifiedPersonReplica verifiedPeople, ProviderIEPublisher publisher, TimeSource time, EventIds eventIds) {
        this.providers = Objects.requireNonNull(providers, "providers");
        this.members = Objects.requireNonNull(members, "members");
        this.verifiedPeople = Objects.requireNonNull(verifiedPeople, "verifiedPeople");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.time = Objects.requireNonNull(time, "time");
        this.eventIds = Objects.requireNonNull(eventIds, "eventIds");
        this.bookability = new TeamBookability(members, publisher, eventIds);
    }

    @Override
    public ReactivateTeamMemberResponse reactivate(ReactivateTeamMemberRequest request) {
        Provider provider = OwnerAuthorization.providerRunBy(providers, request.providerId(), request.actingPersonId());
        TeamMember member = OwnerAuthorization.memberOf(members, provider, request.memberId());
        String displayName = verifiedPeople.findByPersonId(member.personId())
                .orElseThrow(() -> new PersonNotVerifiedException(member.personId().value()))
                .name();
        Instant now = time.now();
        Bookability before = bookability.of(provider);
        member.reactivate(now);
        members.save(member);
        if (member.isActive()) {
            publisher.publish(TeamMemberEvents.activated(eventIds.next(), member, displayName, now));
        }
        Bookability after = bookability.announceChangeFrom(before, provider, now);
        return new ReactivateTeamMemberResponse(
                member.id().value(), provider.id().value(), member.personId().value(), after.isBookable());
    }
}
