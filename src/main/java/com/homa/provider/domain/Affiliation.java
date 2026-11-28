package com.homa.provider.domain;

import com.homa.provider.domain.exception.PersonAlreadyBelongsToProviderException;
import java.util.Optional;

/**
 * What Homa already knows about the provider a person belongs to when they open one or join a team. The rule that a
 * person belongs to only one provider at a time, as its owner or a member, active or deactivated (HLP-10), spans every
 * provider, so the caller looks the person up by personId and passes the answer in; {@link Provider#open} and
 * {@link Provider#addTeamMember} then refuse a person who is already affiliated, as Customer's {@code SignInStatus}
 * does for a sign-in. A removed member belongs to nobody (HLP-8). A unique key on the personId backs the lookup up
 * when two writes race.
 *
 * <p>The owner is affiliated with their own provider, and can also work in its team (PRV-6): that is the same
 * provider, so it is allowed once.
 */
public final class Affiliation {

    private static final Affiliation NONE = new Affiliation(null, null);

    private final ProviderId owns;
    private final ProviderId memberOf;

    private Affiliation(ProviderId owns, ProviderId memberOf) {
        this.owns = owns;
        this.memberOf = memberOf;
    }

    /** The person owns no provider and belongs to no team. */
    public static Affiliation none() {
        return NONE;
    }

    /**
     * What the person belongs to now: the provider they own, if any, and the provider whose team they are on, if any;
     * null for none. Two different providers can only come from data that broke HLP-10 earlier, and either one is
     * refused.
     */
    public static Affiliation of(ProviderId owns, ProviderId memberOf) {
        return new Affiliation(owns, memberOf);
    }

    /** The provider the person owns. Empty if they own none. */
    public Optional<ProviderId> ownedProvider() {
        return Optional.ofNullable(owns);
    }

    /** The provider whose team the person is on, active or deactivated. Empty if they are on none. */
    public Optional<ProviderId> teamProvider() {
        return Optional.ofNullable(memberOf);
    }

    /** Refuses a person who already owns a provider or is on a team from opening a provider (HLP-10). */
    void admitOpeningProvider(PersonId person) {
        if (owns != null || memberOf != null) {
            throw new PersonAlreadyBelongsToProviderException(person.value());
        }
    }

    /**
     * Refuses a person from joining {@code provider}'s team unless they belong to nobody, or own that provider and
     * are not yet on its team (HLP-10, PRV-6). A person already on a team, this one included, is refused.
     */
    void admitJoining(ProviderId provider, PersonId person) {
        if (memberOf != null || (owns != null && !owns.equals(provider))) {
            throw new PersonAlreadyBelongsToProviderException(person.value());
        }
    }
}
