package com.homa.provider.usecase;

import com.homa.provider.domain.Bookability;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.usecase.dto.RemoveTeamMemberRequest;
import com.homa.provider.usecase.dto.RemoveTeamMemberResponse;
import com.homa.provider.usecase.port.in.TeamMemberRemover;
import com.homa.provider.usecase.port.out.EventIds;
import com.homa.provider.usecase.port.out.ProviderIEPublisher;
import com.homa.provider.usecase.port.out.ProviderRepository;
import com.homa.provider.usecase.port.out.RosteredWindowsRepository;
import com.homa.provider.usecase.port.out.TeamMemberRepository;
import com.homa.provider.usecase.port.out.TimeSource;
import java.time.Instant;
import java.util.Objects;

/**
 * The owner removes a member, as it must for one who no longer works for it (HLP-8). The domain refuses while the
 * member is in the Check-in Period or job time of a reservation they're rostered on (HLP-9), judged from the member's
 * rostered windows, which Reservation's events keep. A removed member comes back only as a new member, verified again.
 * {@code TeamMemberDeactivatedIE} with reason REMOVED tells Reservation, which asks the provider to replace them on an
 * upcoming roster (ROS-5), and Listing. When that leaves a live provider without an active helper, it can't be booked,
 * so {@code ProviderBookabilityChangedIE} says so (PRV-2). Only the owner acts on the team (PRV-1).
 */
public final class ProviderRemoveTeamMember implements TeamMemberRemover {

    private final ProviderRepository providers;
    private final TeamMemberRepository members;
    private final RosteredWindowsRepository rosteredWindows;
    private final ProviderIEPublisher publisher;
    private final TimeSource time;
    private final EventIds eventIds;
    private final TeamBookability bookability;

    public ProviderRemoveTeamMember(ProviderRepository providers, TeamMemberRepository members,
            RosteredWindowsRepository rosteredWindows, ProviderIEPublisher publisher, TimeSource time,
            EventIds eventIds) {
        this.providers = Objects.requireNonNull(providers, "providers");
        this.members = Objects.requireNonNull(members, "members");
        this.rosteredWindows = Objects.requireNonNull(rosteredWindows, "rosteredWindows");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.time = Objects.requireNonNull(time, "time");
        this.eventIds = Objects.requireNonNull(eventIds, "eventIds");
        this.bookability = new TeamBookability(members, publisher, eventIds);
    }

    @Override
    public RemoveTeamMemberResponse remove(RemoveTeamMemberRequest request) {
        Provider provider = OwnerAuthorization.providerRunBy(providers, request.providerId(), request.actingPersonId());
        TeamMember member = OwnerAuthorization.memberOf(members, provider, request.memberId());
        Instant now = time.now();
        Bookability before = bookability.of(provider);
        member.remove(rosteredWindows.windowsOf(member.id()), now);
        members.save(member);
        publisher.publish(TeamMemberEvents.deactivated(eventIds.next(), member, TeamMemberEvents.REMOVED, now));
        Bookability after = bookability.announceChangeFrom(before, provider, now);
        return new RemoveTeamMemberResponse(
                member.id().value(), provider.id().value(), member.personId().value(), after.isBookable());
    }
}
