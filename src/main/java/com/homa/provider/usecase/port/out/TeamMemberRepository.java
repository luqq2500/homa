package com.homa.provider.usecase.port.out;

import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.ProviderId;
import com.homa.provider.domain.TeamMember;
import java.util.List;

/** Stores team members, one aggregate per member. Infra rebuilds each one with {@link TeamMember#restore}. */
public interface TeamMemberRepository {

    /** Every member of the provider's team, active or not. */
    List<TeamMember> findByProviderId(ProviderId providerId);

    /**
     * Every member who is this person. Until one provider per person (HLP-10) is enforced in slice 4 there can be more
     * than one; from then on there is at most one, backed by a unique key.
     */
    List<TeamMember> findByPersonId(PersonId personId);

    /** Inserts a new member or replaces the stored one with the same id. */
    void save(TeamMember member);
}
