package com.homa.provider.usecase;

import com.homa.provider.domain.Affiliation;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.ProviderId;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.usecase.port.out.AffiliationLookup;

/**
 * Answers from what the in-memory repositories hold, the way infra answers from its tables: the provider whose owner
 * is the person, and the provider of the team member who is this person and isn't removed. Holds no rule.
 */
public final class InMemoryAffiliationLookup implements AffiliationLookup {

    private final InMemoryProviderRepository providers;
    private final InMemoryTeamMemberRepository members;

    public InMemoryAffiliationLookup(InMemoryProviderRepository providers, InMemoryTeamMemberRepository members) {
        this.providers = providers;
        this.members = members;
    }

    @Override
    public Affiliation of(PersonId personId) {
        ProviderId owns = providers.all().stream()
                .filter(provider -> provider.isRunBy(personId))
                .map(Provider::id)
                .findFirst()
                .orElse(null);
        ProviderId memberOf = members.all().stream()
                .filter(member -> member.personId().equals(personId) && !member.isRemoved())
                .map(TeamMember::providerId)
                .findFirst()
                .orElse(null);
        return Affiliation.of(owns, memberOf);
    }
}
