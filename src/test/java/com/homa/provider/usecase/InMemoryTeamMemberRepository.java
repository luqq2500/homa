package com.homa.provider.usecase;

import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.ProviderId;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.usecase.port.out.TeamMemberRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps a copy of each saved member, in the order first saved, and hands out fresh copies on every read, as a
 * database would. Holds no rule.
 */
public final class InMemoryTeamMemberRepository implements TeamMemberRepository {

    private final Map<TeamMemberId, TeamMember> stored = new LinkedHashMap<>();

    @Override
    public Optional<TeamMember> findById(TeamMemberId id) {
        return Optional.ofNullable(stored.get(id)).map(InMemoryTeamMemberRepository::copyOf);
    }

    @Override
    public List<TeamMember> findByProviderId(ProviderId providerId) {
        return stored.values().stream()
                .filter(member -> member.providerId().equals(providerId))
                .map(InMemoryTeamMemberRepository::copyOf)
                .toList();
    }

    @Override
    public List<TeamMember> findByPersonId(PersonId personId) {
        return stored.values().stream()
                .filter(member -> member.personId().equals(personId))
                .map(InMemoryTeamMemberRepository::copyOf)
                .toList();
    }

    @Override
    public void save(TeamMember member) {
        stored.put(member.id(), copyOf(member));
    }

    /** The stored member, for assertions. */
    public TeamMember stored(TeamMemberId id) {
        return copyOf(stored.get(id));
    }

    /** Every stored member, in the order first saved. */
    public List<TeamMember> all() {
        return stored.values().stream().map(InMemoryTeamMemberRepository::copyOf).toList();
    }

    private static TeamMember copyOf(TeamMember member) {
        return TeamMember.restore(member.id(), member.providerId(), member.personId(), member.role(),
                member.eligibility(), member.addedAt(), member.hasVerifiedPhoto(), member.isDeactivated(),
                member.isSuspended(), member.isRemoved(), member.verifiedUntil());
    }
}
