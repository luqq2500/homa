package com.homa.provider.usecase.port.out;

import com.homa.provider.domain.Affiliation;
import com.homa.provider.domain.PersonId;

/**
 * Finds the provider a person belongs to, across every provider, so that one person belongs to only one at a time
 * (HLP-10). A person belongs to a provider as its owner or as a member of its team, active or deactivated; a removed
 * member belongs to nobody (HLP-8). The use cases pass the answer to the domain as an {@link Affiliation}, which
 * refuses.
 *
 * <p>Infra answers it from the providers' owners and the team members who aren't removed. A unique key on each backs
 * it up when two writes race: one provider per owner personId, and one team member per personId among those who aren't
 * removed.
 */
public interface AffiliationLookup {

    /** What the person owns and which team they are on now; {@link Affiliation#none()} when neither. */
    Affiliation of(PersonId personId);
}
