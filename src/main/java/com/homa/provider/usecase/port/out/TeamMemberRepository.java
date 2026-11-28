package com.homa.provider.usecase.port.out;

import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.ProviderId;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.domain.TeamMemberId;
import java.util.List;
import java.util.Optional;

/** Stores team members, one aggregate per member. Infra rebuilds each one with {@link TeamMember#restore}. */
public interface TeamMemberRepository {

    Optional<TeamMember> findById(TeamMemberId id);

    /**
     * Every member of the provider's team: active, deactivated and suspended, and the removed ones too, kept as
     * history (HLP-8). A removed member is never active, so counting active helpers is not affected.
     */
    List<TeamMember> findByProviderId(ProviderId providerId);

    /**
     * Every member who is this person, the removed ones included. A person who was removed and added again as a new
     * member (HLP-8) has the old, removed member and the new one. At most one of them isn't removed (HLP-10), backed by
     * a unique key on the personId among those who aren't.
     */
    List<TeamMember> findByPersonId(PersonId personId);

    /** Inserts a new member or replaces the stored one with the same id. */
    void save(TeamMember member);
}
